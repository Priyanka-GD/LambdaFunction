import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import * as path from 'path';

export class MyCdkProjectStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        // Create the Lambda Function (Java)
       const myLambda = new lambda.Function(this, 'MyJavaLambdaFunction', {
           runtime: lambda.Runtime.JAVA_17,
           handler: 'org.example.lambda.LambdaHandler::handleRequest', // Ensure this matches the Java package
           code: lambda.Code.fromAsset('../target/LambdaFunction-1.0-SNAPSHOT.jar'), // ✅ Corrected path
           memorySize: 512,
           timeout: cdk.Duration.seconds(10)
       });

        // Create API Gateway and integrate with Lambda
        const api = new apigateway.LambdaRestApi(this, 'MyApiGateway', {
            handler: myLambda,
            proxy: true, // Allows direct invocation of the Lambda
        });
        // Grant Bedrock Invoke Permissions to Lambda
         const bedrockPolicy = new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                    "bedrock:InvokeModel",
                    "bedrock:ListFoundationModels"
              ],
              resources: ["*"] // Can restrict to specific model ARN if needed
            });

         //Attach IAM Policy to Lambda
         myLambda.addToRolePolicy(bedrockPolicy);

        // Output API Gateway URL
        new cdk.CfnOutput(this, 'ApiGatewayUrl', {
            value: api.url,
            description: 'API Gateway Endpoint URL',
        });
    }
}
