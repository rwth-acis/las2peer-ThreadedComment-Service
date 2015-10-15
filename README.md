![LAS2peer](https://github.com/rwth-acis/LAS2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

LAS2peer-CommentService
=======================

This is a [LAS2peer](https://github.com/rwth-acis/las2peer/) service allowing other services to provide comment functionality.

Other services can create a comment thread (via RMI) and set up permissions. Users post comment to this thread using a REST api.
The service makes heavy use of the distributed storage of las2peer. All user content is encrypted using UserAgents and GroupAgents in order to be independent from specific ServiceAgents.
There is also a CommentExampleService to show how to call the CommentService's RMI methods.
ROLE widgets are provided for both services.

Features
--------

* add, edit and delete comments
* administer comments (thread owner can remove comments)
* reply to comments
* up/downvote comments
* permissions (owner = admin, writer = post comments, reply and vote, reader = read-only access)
* RMI methods to create and delete threads
