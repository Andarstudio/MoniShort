package app.monishort.automation

/** A user-facing failure of an action (bad parameter, missing permission, network error...). */
class ActionException(message: String) : Exception(message)

/** Thrown when a workflow exceeds its step/iteration limits. */
class ExecutionLimitException(message: String) : Exception(message)
