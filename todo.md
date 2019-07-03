* better error handling in general
	* case when ssh key is invalid is troublesome
	* SingleRunService does not communicate well with ServiceOutputActivity, especially on error
		* in general, behaviour on error needs to be better defined
	* misc. foolproofing: e.g., no pausing/resuming after an error
* thorough testing
* code refactoring
