* service/activity communication needs to be much safer and more carefully implemented (especially between `SingleRunService` and `ServiceOutputAcitivity`); not sure if `IntentService` is suitable for this situation
* service state is not properly synced with activity state (e.g., if an error occurs, `ControlActivity` is not immediately updated)
	* leads to problems with auto-restart option
* see if `ControlActivity` can work properly without `android:excludeFromRecents="true"`
* thorough testing
* major code refactoring
