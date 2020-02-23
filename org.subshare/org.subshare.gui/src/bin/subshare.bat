@echo off

REM java -jar "%~dp0\..\lib\@project.artifactId@-@project.version@.jar" %*
java --add-opens java.base/java.util=co.codewizards.cloudstore.core --module-path "%~dp0\..\lib" --module "org.subshare.gui/org.subshare.gui.SubShareGui" %*

REM The command created the 'updater' directory, if it detected that an update must be done.

if exist "%~dp0\..\updater" goto update
goto end

:update

REM The primary command already copied the entire installation into this 'updater' sub-directory.
REM This way, the real installation can be completely overwritten. I tested it and it seems that
REM a batch file can be overwritten while it is being executed (fortunately, this is an exception
REM to the ordinary Windows file blocking bullshit).

echo Updating Subshare...
REM java -jar "%~dp0\..\updater\lib\org.subshare.updater.gui-@project.version@.jar" -installationDir "%~dp0\.."
java --module-path "%~dp0\..\updater\lib" --module "org.subshare.updater.gui/org.subshare.updater.gui.SubShareUpdaterGui" -installationDir "%~dp0\.."
"%0" afterUpdateHook

:end
