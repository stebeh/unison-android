* service/activity communication needs to be much safer and more carefully implemented (especially between `SingleRunService` and `ServiceOutputAcitivity`); not sure if `IntentService` is suitable for this situation
* see if `ControlActivity` can work properly without `android:excludeFromRecents="true"`
* thorough testing
* major code refactoring
