yas
===

yas is a simple version control system

I am trying to learn java doing this small project with it. it is supposed to be a version control system.
It is inspired heavily by "git".

Usage: DO NOT ACTUALLY USE THIS. IT MAY BE BROKEN IN WAYS YOU CANT EVEN IMAGINE

If you want to give it a try; (Assuming yas.Yas is in your classpath)

java yas.Yas init -> initialize empty yas repository
java yas.Yas commit "commit message" -> takes a snapshot of current working directory. There is no staging or tracking
                                        everything will be added.
                                        
java yas.Yas log -> prints a log of commits

java yas.Yas reset -> reset everything to their state in last commit
java yas.Yas reset 9d3160bf48c711867f0db139da327f1dd2e48281 -> go back to specified commit

a note about resetting: resetting works overwriting previous version of files into current versions.
If there were no previous versions of a file, it will not be touched.
