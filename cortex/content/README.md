# Content
The zip file contains all the files necessary to test and train the german credit model.  It was encrypted using the
cli and running the following commands.  

Note:  The password is **german-credit**
```shell
(smoke-tests) rhogan@C02X4GP4JG5J cortex % zip -erj german-credit.zip content
Enter password: 
Verify password: 
  adding: german_credit_explan.csv (deflated 91%)
  adding: german_credit_eval.csv (deflated 94%)
  adding: german_credit_test.csv (deflated 93%)
  adding: scan.yaml (deflated 57%)
(smoke-tests) rhogan@C02X4GP4JG5J cortex % ls

```

The file scan.yaml is required if you want to configure and run a Certifai scan on the model.  _It is not included as
part of this smoke test but may be needed in the future_