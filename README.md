# AI2_SSH_EXTENSION
Android client SSH extension for MIT Application Inventor 2

USAGE:
=====
1 ) Fill string variables "SetUser", "SetHost" and "SetPasswords" accordingly, without queotes

2 ) Fill string variable "Command" with the Bash/Sheel command to be issued, without queotes (e.g: ls )

3 ) Call the function "SendData"

4 ) Get result either:

4.1 ) ...from within "NewIncomingMessage" event, argument "data" contains current text line, indded the last one.

4.1 ) ...by reading "GetReceivedMessage" variable; be aware that it is assyncronously updated (intermediary values are superseded)
