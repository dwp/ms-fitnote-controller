<a name="1.6.0"></a>
# 1.6.0 (2021-07-08)


### Features

* **logging:** add logging for failure reasons ([6d3e2918](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/6d3e2918))
* **test:** update nino in cucumber tests ([86950b77](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/86950b77))


<a name="1.5.0"></a>
# 1.5.0 (2021-02-17)


### Features

* **owasp:** update vulnerabilities ([5f9437e4](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/5f9437e4))


<a name="1.4.0"></a>
# 1.4.0 (2021-02-17)


### Features

* **feat:** removed barcode analyser ([c68faf69](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c68faf69))
* **feat:** add internal error responses ([c68faf69](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c68faf69))
* **feat:** externalise config options ([c68faf69](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c68faf69))
* **refactor:** refactor resources ([c68faf69](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c68faf69))


<a name="1.3.1"></a>
# 1.3.1 (2019-10-09)


### Features

* **owasp:** update vulnerabilities ([ebce62d](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/ebce62d))



<a name="1.3.0"></a>
# 1.3.0 (2019-10-07)


### Bug Fixes

* **versions:** applying new commits to merged work ([5e1360c](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/5e1360c))


### Features

* **lint:** updated linting and removing empty class file (that should have been removed during the merge) ([032b729](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/032b729))



<a name="1.2.0"></a>
# 1.2.0 (2019-09-13)



<a name="1.1.0"></a>
# 1.1.0 (2019-09-13)



<a name="1.0.0"></a>
# 1.0.0 (2019-05-09)



<a name="0.21.0"></a>
# 0.21.0 (2019-05-08)


### Bug Fixes

* **owasp:** fix jetty vulnerability ([58c3170](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/58c3170))
* **owasp:** updating jetty vulnerability ([f5b8297](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/f5b8297))
* **pdf:** fixed pdfbox (new version) which has 0 DPI passing even though it is a blank image ([efc762f](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/efc762f))
* **version:** fixed owasp versions for pdfbox and owasp itself (4.0.2) ([6212a0c](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/6212a0c))
* **version:** updating dependency versions ([48d7427](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/48d7427))


### Features

* **old-style:** create new configuration parameter 'ocrVerticalSlice' to allow the TL, TR, BL and BR slice of the ocr image to be made larger or smaller.  This is because of an old-style fitnote that has been received which has a large margin at the top and bottom of the page.  Allowing a larger vertical slice will take longer to process but can be traded off against being unable to ocr the fitnote at all ([10c8468](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/10c8468))
* **sns:** remove rabbitmq configurations and usage and replaced with SNS ([5d316ab](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/5d316ab))
* **version:** update versions and remove qpid ([9c82434](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/9c82434))



<a name="0.20.0"></a>
# 0.20.0 (2018-10-04)


### Features

* **deliveryMode:** include the new version of event-management and add some tests to verify they are persistent ([9452dc0](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/9452dc0))



<a name="0.19.0"></a>
# 0.19.0 (2018-09-10)


### Bug Fixes

* **owasp:** upgraded jetty ([f75f02a](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/f75f02a))
* **vulnerabilities:** address further synk identified vulnerabilities ([28e1435](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/28e1435))


### Features

* **timing:** do no use Thread.sleep, instead use TimeUnit ([5cb6fda](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/5cb6fda))



<a name="0.18.0"></a>
# 0.18.0 (2018-08-08)


### Bug Fixes

* **cukes:** remove accidentally included tag, test should run ALL ([3b50f64](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/3b50f64))
* **submit:** correct error log entry ([a893e62](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/a893e62))
* **synk:** fixing snyk identified vulnerabilities for jetty-http and pdfbox ([4414d08](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/4414d08))
* **typo:** correct spelling of diagonal ([62cdcae](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/62cdcae))


### Features

* **address:** add in address query and tests ([ce1687a](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/ce1687a))
* **app:** remove unused properties and parameters ([8673806](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/8673806))
* **config:** declare and wire in the FitnoteQueryResource to the main application ([8b1006b](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/8b1006b))
* **cucumber:** add all cucumber tests for mobile, address and nino checks ([0db54f1](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/0db54f1))
* **mobile:** add QueryMobile view to ImagePayload ([21aec41](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/21aec41))
* **mobile:** implement mobile query and tests ([9a8fe57](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/9a8fe57))
* **nino:** test class and main implementation for query nino functionality ([f304d48](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/f304d48))
* **session:** add endpoint and functionality to extend the session timout in redis ([c06411a](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c06411a))
* **views:** add QueryAddress to the class property views for serialisation ([6c98271](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/6c98271))
* **views:** implement nino view interface to ImagePayload properties ([cc0a616](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/cc0a616))



<a name="0.17.0"></a>
# 0.17.0 (2018-07-19)


### Bug Fixes

* **json-logging:** suppress the bootstrapped logger ([d045d24](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/d045d24))
* **log:** better logging of relevant stacks when clearExpiredObjects() is called ([8470e65](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/8470e65))
* **logging:** additional logging for image sizing and sessions ([1156a9c](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/1156a9c))
* **logging:** clear the image to free up memeory when it is rejected at OCR ([9e438e8](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/9e438e8))
* **logging:** decreased the session-check frequency and remove a stray comma from a comment ([dccfa6c](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/dccfa6c))
* **logging:** further logging for the images and hashes ([648010a](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/648010a))
* **logging:** more logging for OCR response time ([12812f7](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/12812f7))
* **logging:** more logging for OCR response time ([299b9c4](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/299b9c4))
* **logging:** removed a stray comma from a comment ([cc873c6](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/cc873c6))
* **messaging:** fixed the connection publish/subscribe model using the 1.7.0-BETA version of RabbitMQ util ([b2a0526](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/b2a0526))
* **redis:** remove the need for any threading tests to mange the expiry of objects.  this will be hanled by redis internally ([98be650](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/98be650))
* **serialisation:** fixed serialisation of ninoObject ([e2f6016](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/e2f6016))
* **serialisation:** serialisation of ImagePayload in/out of redis for ninoObject ([428ae47](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/428ae47))
* **sizing:** resiliance for redis connections and clear image on failure ([c44b007](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/c44b007))
* **thread:** explicitly declared 4 threads for ocr rotations and wait for them to terminate before returning back to the application to ensure they are closed.  have run this against process map and checks out ok with the heap space reuse ([1cbce16](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/1cbce16))
* **thread:** send termination but do not wait for them, the framework will terminate them as the ExecutionService drifts out of scope ([21f65ca](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/21f65ca))
* **transaction:** there is little point in using a transaction as it is only a single command.  the transaction functionality is to atomically execute a number of commands so this is probably not applicable ([b56ac79](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/b56ac79))
* **version:** fix jetty version and redact Dockerfile and Jenkinsfile from release ([871b1fb](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/871b1fb))
* **version:** up-issue the version for the rabbitmq events manager ([f17285a](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/f17285a))
* **version:** update pom to correct mock version ([8088c6e](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/8088c6e))
* **versions:** update all versions to the latest for dropwizard, remove jackson-databind reference and update yml files with the new configuration ([18b91d2](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/18b91d2))


### Features

* **crypto:** implementing kms encryption/decryption on redis objects ([71e9ac2](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/71e9ac2))
* **logging:** address sonar issues for logging ([433ccbc](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/433ccbc))
* **logging:** extra logging at ERROR level when something goes wrong.  log the exception message at error level and a stack trace at debug ([bfd5e23](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/bfd5e23))
* **logging:** turn bytes to mega-bytes for readability ([4cbde5d](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/4cbde5d))
* **memory:** surrounding try/with/resources on ByteArrayOutput/Input streams ([29b3477](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/29b3477))
* **messaging:** updating the interface to use the rabbitmq-events-manager dependency now that the cipher has been extracted to a separate utility ([64fc148](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/64fc148))
* **redis:** added extra checks for image and address through to redis... now it is not a memory object it cannot be assumed that the ImagePayload object will update without writiing back to Redis ([25de244](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/25de244))
* **redis:** adding 'Jedis' and 'Mock-Jedis' to replace the in-memory Hashmaps for ImagePayload and ImageHashStore.  This unit tests are in place with the mock but I am still looking for a working embedded redis that can be used for the integration cucumber tests.  the search continues that doesn't cause a firewall issue by starting .exe programs in the background ([497e8c4](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/497e8c4))
* **redis:** additional mods to prepare the controller for integration to redis ([e194153](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/e194153))
* **redis:** implementing the in-memory redis and the use of lettuce.  much more robust ([ccc0771](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/ccc0771))
* **update:** new updates to support both the encoded-logger but also the messaging ([271e08e](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/271e08e))
* **validation:** update mobile phone validation acceptable lengths ([a19f8d1](https://gitlab.nonprod.dwpcloud.uk/health-pdu/fitnote/fitnote-controller/commit/a19f8d1))



