# RPiCronPing
App that sends a notification to a given user/group, using AWS Lambda with a scheduled trigger.

Easy setup:
1. Do the changes.
2. Create the package with `mvn clean package` command.
3. Upload the .jar file from ./target to AWS Lambda Function.
4. Make sure the Lambda uses the handler from `Handler::handleRequest`