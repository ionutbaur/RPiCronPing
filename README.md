# RPiCronPing
App that sends a notification to a given user/group, using AWS Lambda with a scheduled trigger.

Easy setup:
1. Do the changes.
2. Create the package with `mvn clean package` command.
3. Upload the .jar file from ./target to AWS Lambda Function.
4. Add the following environment variables to Lambda function:
   1. depending on the type of notification needed:
      * for **Mail** notifications:
        * `MAIL_FROM`
        * `MAIL_TO`
        * `MAIL_SUBJECT` (will default to "Raspberry Pi status" if not provided)
      * for **Phone** notifications:
          * `PHONE`
   2. for any kind of notification:
      * `NOTIFICATION_TYPE` ("MAIL" / "PHONE" - will default to "MAIL" if not provided)
      * `HOST`
      * `PORT`
      * `BEGIN_IGNORED_HOUR`
      * `BEGIN_IGNORED_MINUTE`
      * `END_IGNORED_HOUR`
      * `END_IGNORED_MINUTE`
        
5. Make sure the Lambda uses the handler from `Handler::handleRequest`