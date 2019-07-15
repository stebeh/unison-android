# Unison Android App

This app brings the [Unison](https://github.com/bcpierce00/unison) file-synchronization utility to Android. It can run Unison either on demand or as a background process (`-repeat watch`) that is simple to manage.

## Screenshots

<image src=screens/screen1.png width=200x/>&nbsp;&nbsp;
<image src=screens/screen2.png width=200x/>&nbsp;&nbsp;
<image src=screens/screen3.png width=200x/>&nbsp;&nbsp;
<image src=screens/screen4.png width=200x/>&nbsp;&nbsp;

## Usage

The app needs to be provided with a Unison profile; see the [sample profile](sample.prf) or refer to the [manual](http://www.cis.upenn.edu/~bcpierce/unison/download/releases/stable/unison-manual.html) for advanced options. Note that many options are untested and may cause unexpected behaviour.

If your profile has a remote root, you will need to also select an (OpenSSH) key file in order to establish passwordless ssh connections. This is the private component of the key pair generated by `ssh-keygen` (and normally resides in `~/.ssh/id_rsa` on Linux systems). Please see [https://www.ssh.com/ssh/keygen/](https://www.ssh.com/ssh/keygen/) for more information.

## (Known) limitations and bugs

* Only non-interactive (`-batch`) mode is supported.
* Limited handling of various errors and edge cases (see [todo](todo.md)).
* Not yet thoroughly tested.


## Acknowledgements

[Unison](https://github.com/bcpierce00/unison) is written by Benjamin C. Pierce.

Android binaries of the program taken from [https://github.com/vovcacik/unison-build-scripts](https://github.com/vovcacik/unison-build-scripts).

Icons provided by [https://icons8.com/](https://icons8.com/).

## Contribute

Contributions are welcome.