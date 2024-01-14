package dev.crashteam.uzumanalytics.stream.listener.aws.model

import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput

data class RestartPaymentStreamEvent(val shutdownInput: ShutdownInput)
