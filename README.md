# DEPRECATED
## For integration with Jenkins, please see the integration documentation located https://apifortress.com/doc/jenkins-integrate-cicd/. APIF-Auto, a Python-based command line tool can also be used, learn how here https://apifortress.com/doc/apif-auto-and-jenkins/.

# API Fortress Jenkins plugin
This plugin allows you to add an API testing build step in your Jenkins CI.
There are multiple testing modes you can choose from, depending on your needs.

The key parameter that you need to retrieve from API Fortress for any mode, is the Hook URL,
a unique generated URL representing a project. You can create as many as you want in the company
settings of your API Fortress trial/subscription account.

## Modes
There are 4 testing modes available. "Run single test" will run a single test, while the others will
run suites of tests.

### Run single test
You can run a single test by providing a test ID. Test IDs can be found in test interstitial pages.

### Run automatch
You can run our "automatch" mode by providing a URL representing a certain endpoint. You can configure
automatch patterns in the "Automatch" section of each test.
Read more about automatch [Here](http://apifortress.com/doc/automatch/) .

### Run by tag
By running "by tag" you can run multiple tests, marked with a certain tag. Tags can be added and edited
in the test details.

### Run project
By running a full project, you'll be running all tests contained in the project.

## Options
The following options can apply to any mode.

### Blocking
The plugin can silently run (blocking = false), let the build continue with a success and inform you if the
test failed using the various methods available. Or it can be actively determining the build success (blocking=true)
so that the build will wait for the tests result and stop the build if the tests fail.

### Dry-run
If checked, no events will be stored within API Fortress. To be used in conjuection with "blocking".

### Silent
IF checked, no alerts will be sent if the tests fail.

### Parameters override
You can override up to 3 parameters in the test scope. These variables will appear within the test scope just like
any other variable. Pretty useful if you're willing to override the domain of the service being tested (Ie. staging vs production).
