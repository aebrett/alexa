# Alexa Skills in scala

## Usage
The following environment variables are required to enable automatic lambda creation:

- AWS_ACCESS_KEY_ID (the API access key for an IAM admin user)
- AWS_SECRET_ACCESS_KEY (the API secret key for an IAM admin user)
- AWS_LAMBDA_BUCKET_ID (the name of an existing S3 bucket, eg. "alexa-scala")
- AWS_LAMBDA_IAM_ROLE_ARN (optional, the arn for an IAM role in which the lambda will be executed,
  of the form "arn:aws:iam::xxxxxxxxx:role/lambda_basic_execution")

To create one or more lambdas for the first time, run 'sbt createLambda'. 
Subsequently, to update them run 'sbt updateLambda'.

Once the lambda is created (which must be in the "us-east-1" or "eu-west-1" regions), use the Lambda web console
to add an Alexa Skills trigger. Then use the Alexa Skills console to add a skill that connects to the Lambda, and
manually add an interaction model using the intent schema and sample utterances in the project.