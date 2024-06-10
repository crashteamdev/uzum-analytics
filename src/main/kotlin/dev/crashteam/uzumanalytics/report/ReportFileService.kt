package dev.crashteam.uzumanalytics.report

import dev.crashteam.uzumanalytics.db.model.enums.ReportStatus
import dev.crashteam.uzumanalytics.report.model.CustomCellStyle
import dev.crashteam.uzumanalytics.report.model.Report
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChProductSalesReport
import dev.crashteam.uzumanalytics.repository.postgres.ReportRepository
import dev.crashteam.uzumanalytics.service.ProductServiceV2
import dev.crashteam.uzumanalytics.service.model.AggregateSalesProduct
import mu.KotlinLogging
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFFont
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.io.OutputStream
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class ReportFileService(
    private val reportRepository: ReportRepository,
    private val productServiceV2: ProductServiceV2,
    private val stylesGenerator: StylesGenerator,
) {

    private val headerNames = arrayOf(
        "Продавец", "ID продукта", "Название",
        "Категория", "Остаток", "Дней в наличии",
        "Цена", "Заказов", "Выручка", "ABC заказы", "ABC выручка"
    )

    suspend fun saveReport(
        jobId: String,
        fileInputStream: InputStream,
    ): String {
        val reportId = reportRepository.saveJobIdFile(jobId, fileInputStream)
            ?: throw IllegalStateException("Empty report id")
        return reportId
    }

    suspend fun getReport(jobId: String): Report? {
        val fileByteArray = reportRepository.getFileByJobId(jobId) ?: return null

        return Report("unknown", fileByteArray.inputStream())
    }

    @Transactional
    suspend fun removeFileOlderThan(maxDataTime: LocalDateTime) {
        reportRepository.removeAllJobFileBeforeDate(maxDataTime)
        for (reports in reportRepository.findAllCreatedLessThan(maxDataTime)) {
            reportRepository.updateReportStatusByJobId(reports.jobId, ReportStatus.deleted)
        }
    }

    suspend fun generateReportBySellerV2(
        link: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        outputStream: OutputStream
    ) {
        SXSSFWorkbook().use { wb ->
            val styles = stylesGenerator.prepareStyles(wb)
            val sheet: SXSSFSheet = wb.createSheet("ABC отчет")
            wb.createSheet("marketdb.org")
            wb.createSheet("Report range - ${Duration.between(fromTime, toTime).toDays()}")
            val sumCurrencyCellFormat = sumCurrencyCellFormat(wb)
            val linkFont: Font = wb.createFont().apply {
                this.underline = XSSFFont.U_SINGLE
                this.color = HSSFColor.HSSFColorPredefined.BLUE.index
            }
            val linkStyle: CellStyle = wb.createCellStyle().apply { setFont(linkFont) }

            createHeaderRow(sheet, styles, headerNames)

            val limit = 10000
            var offset = 0
            var total = 0L
            var rowCursor = 0
            var columnCursor = 1

            while (true) {
                if (offset != 0 && offset >= total) break

                val sellerSales: List<ChProductSalesReport> =
                    productServiceV2.getSellerSales(link, fromTime, toTime, limit, offset)
                total = sellerSales.first().total

                if (sellerSales.isEmpty()) break

                val totalRowCount = total + 1
                for (sellerSale in sellerSales) {
                    rowCursor++
                    columnCursor++
                    fullWorkBookContentV2(
                        sellerSale,
                        rowCursor,
                        columnCursor,
                        totalRowCount,
                        sheet,
                        sumCurrencyCellFormat,
                        linkStyle,
                        wb
                    )
                }
                offset += limit
            }
            sheet.trackAllColumnsForAutoSizing()
            for (i in headerNames.indices) {
                sheet.autoSizeColumn(i)
            }
            outputStream.use { wb.write(it) }
            wb.dispose()
        }
    }

    suspend fun generateReportByCategoryV2(
        categoryId: Long,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        outputStream: OutputStream
    ) {
        SXSSFWorkbook().use { wb ->
            val styles = stylesGenerator.prepareStyles(wb)
            val sheet: SXSSFSheet = wb.createSheet("ABC отчет")
            wb.createSheet("marketdb.org")
            wb.createSheet("Report range - ${Duration.between(fromTime, toTime).toDays()}")
            val sumCurrencyCellFormat = sumCurrencyCellFormat(wb)
            val linkFont: Font = wb.createFont().apply {
                this.underline = XSSFFont.U_SINGLE
                this.color = HSSFColor.HSSFColorPredefined.BLUE.index
            }
            val linkStyle: CellStyle = wb.createCellStyle().apply { setFont(linkFont) }

            createHeaderRow(sheet, styles, headerNames)

            val limit = 10000
            var offset = 0
            var total = 0L
            var rowCursor = 0
            var columnCursor = 1

            while (true) {
                if (offset != 0 && offset >= total) break

                val categorySales: List<ChProductSalesReport> = productServiceV2.getCategorySales(
                    categoryId = categoryId,
                    fromTime = fromTime,
                    toTime = toTime,
                    limit = limit,
                    offset = offset
                )
                total = categorySales.first().total

                if (categorySales.isEmpty()) break

                log.info {
                    "Received category sales. categoryId=$categoryId" +
                            " limit=$limit; offset=$offset" +
                            " size=${categorySales.size};"
                }

                if (categorySales.isEmpty()) {
                    log.info {
                        "Empty category sales, finishing. categoryId=$categoryId" +
                                " limit=$limit; offset=$offset"
                    }
                    break
                }

                val totalRowCount = total + 1
                for (sellerSale in categorySales) {
                    rowCursor++
                    columnCursor++
                    fullWorkBookContentV2(
                        sellerSale,
                        rowCursor,
                        columnCursor,
                        totalRowCount,
                        sheet,
                        sumCurrencyCellFormat,
                        linkStyle,
                        wb
                    )
                }
                offset += limit
            }

            sheet.trackAllColumnsForAutoSizing()
            for (i in headerNames.indices) {
                sheet.autoSizeColumn(i)
            }
            outputStream.use { wb.write(it) }
            wb.dispose()
        }
    }

    private fun fullWorkBookContentV2(
        sellerSale: ChProductSalesReport,
        rowCursor: Int,
        columnCursor: Int,
        totalRowCount: Long,
        sheet: SXSSFSheet,
        rubleCurrencyCellFormat: CellStyle?,
        linkStyle: CellStyle,
        wb: Workbook,
    ) {
        val row = sheet.createRow(rowCursor)
        for (i in headerNames.indices) {
            val cell = row.createCell(i)
            when (i) {
                0 -> cell.setCellValue(sellerSale.sellerTitle)
                1 -> {
                    cell.setCellValue(sellerSale.productId)
                    val link = wb.creationHelper.createHyperlink(HyperlinkType.URL)
                    link.address = "https://uzum.uz/ru/product/${sellerSale.productId}"
                    cell.hyperlink = link
                    cell.cellStyle = linkStyle
                }

                2 -> cell.setCellValue(sellerSale.name)
                3 -> cell.setCellValue(sellerSale.categoryName)
                4 -> cell.setCellValue(sellerSale.availableAmounts.toDouble())
                5 -> {
                    val daysInStock = sellerSale.availableAmountGraph.filter { it > 0 }
                    cell.setCellValue(daysInStock.size.toDouble())
                }

                6 -> {
                    cell.cellStyle = rubleCurrencyCellFormat
                    cell.setCellValue(sellerSale.purchasePrice.toDouble())
                }

                7 -> {
                    cell.setCellValue(sellerSale.orderGraph.map { it }
                        .reduce { o, o2 -> o.plus(o2) }.toDouble())
                }

                8 -> {
                    cell.cellStyle = rubleCurrencyCellFormat
                    cell.setCellValue(
                        sellerSale.sales.setScale(2, RoundingMode.HALF_UP).toDouble()
                    )
                }

                9 -> {
                    cell.cellFormula =
                        "CHOOSE(MATCH((SUMIF(\$H\$2:\$H$totalRowCount,\">\"&\$H$columnCursor)+\$H$columnCursor)/SUM(\$H\$2:\$H$totalRowCount),{0,0.81,0.96}),\"A\",\"B\",\"C\")"
                }

                10 -> {
                    cell.cellFormula =
                        "CHOOSE(MATCH((SUMIF(\$I\$2:\$J$totalRowCount,\">\"&\$I$columnCursor)+\$I$columnCursor)/SUM(\$I\$2:\$I$totalRowCount),{0,0.81,0.96}),\"A\",\"B\",\"C\")"
                }
            }
        }
    }

    private fun fillWorkBookData(sheet: SXSSFSheet, wb: Workbook, data: List<AggregateSalesProduct>) {
        val sumCurrencyCellFormat = sumCurrencyCellFormat(wb)
        val rowCount = data.size + 1
        var rowCursor = 0
        var columnCursor = 1
        val helper: CreationHelper = wb.creationHelper
        val linkStyle: CellStyle = wb.createCellStyle()
        val linkFont: Font = wb.createFont()

        // Setting the Link Style
        linkFont.underline = XSSFFont.U_SINGLE
        linkFont.color = HSSFColor.HSSFColorPredefined.BLUE.index
        linkStyle.setFont(linkFont)
        for (sellerSale in data) {
            rowCursor++
            columnCursor++
            val row = sheet.createRow(rowCursor)
            for (i in headerNames.indices) {
                val cell = row.createCell(i)
                when (i) {
                    0 -> cell.setCellValue(sellerSale.seller.name)
                    1 -> {
                        cell.setCellValue(sellerSale.productId.toString())
                        val link = helper.createHyperlink(HyperlinkType.URL)
                        link.address = "https://uzum.uz/ru/product/${sellerSale.productId}"
                        cell.hyperlink = link
                        cell.cellStyle = linkStyle
                    }

                    2 -> cell.setCellValue(sellerSale.skuId.toString())
                    3 -> cell.setCellValue(sellerSale.name)
                    4 -> cell.setCellValue(sellerSale.category.name)
                    5 -> cell.setCellValue(sellerSale.availableAmount.toDouble())
                    6 -> cell.setCellValue(sellerSale.daysInStock.toDouble())
                    7 -> {
                        cell.cellStyle = sumCurrencyCellFormat
                        cell.setCellValue(sellerSale.price.toDouble())
                    }

                    8 -> {
                        cell.setCellValue(sellerSale.orderGraph.reduce { o, o2 -> o.plus(o2) }.toDouble())
                    }

                    9 -> {
                        cell.cellStyle = sumCurrencyCellFormat
                        cell.setCellValue(
                            sellerSale.proceeds.setScale(2, RoundingMode.HALF_UP).toDouble()
                        )
                    }

                    10 -> {
                        cell.cellFormula =
                            "CHOOSE(MATCH((SUMIF(\$I\$2:\$I$rowCount,\">\"&\$I$columnCursor)+\$I$columnCursor)/SUM(\$I\$2:\$I$rowCount),{0,0.81,0.96}),\"A\",\"B\",\"C\")"
                    }

                    11 -> {
                        cell.cellFormula =
                            "CHOOSE(MATCH((SUMIF(\$J\$2:\$J$rowCount,\">\"&\$J$columnCursor)+\$J$columnCursor)/SUM(\$J\$2:\$J$rowCount),{0,0.81,0.96}),\"A\",\"B\",\"C\")"
                    }
                }
            }
        }
        sheet.trackAllColumnsForAutoSizing()
        for (i in headerNames.indices) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun sumCurrencyCellFormat(wb: Workbook): CellStyle? {
        val cellStyle = wb.createCellStyle()
        val format: DataFormat = wb.createDataFormat()
        cellStyle.dataFormat = format.getFormat("#,#0.00")

        return cellStyle
    }

    private fun createHeaderRow(
        sheet: Sheet,
        styles: Map<CustomCellStyle, CellStyle>,
        columnNames: Array<String>
    ) {
        val row = sheet.createRow(0)

        columnNames.forEachIndexed { index, name ->
            val cell = row.createCell(index)
            sheet.setColumnWidth(index, 256 * 15)

            cell.setCellValue(name)
            cell.cellStyle = styles[CustomCellStyle.GREY_CENTERED_BOLD_ARIAL_WITH_BORDER]
        }
    }

}
