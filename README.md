<p align="center">
  <img src="https://raw.githubusercontent.com/rwth-acis/las2peer/master/img/logo/bitmap/las2peer-logo-128x128.png" />
</p>
<h1 align="center">las2peer-ThreadedComment-Service</h1>
<p align="center">
  <a href="https://travis-ci.org/rwth-acis/las2peer-ThreadedComment-Service" alt="Travis Build Status">
        <img src="https://travis-ci.org/rwth-acis/las2peer-ThreadedComment-Service.svg?branch=master" /></a>
  <a href="https://codecov.io/gh/rwth-acis/las2peer-ThreadedComment-Service" alt="Code Coverage">
        <img src="https://codecov.io/gh/rwth-acis/las2peer-ThreadedComment-Service/branch/master/graph/badge.svg" /></a>
  <a href="https://libraries.io/github/rwth-acis/las2peer-ThreadedComment-Service" alt="Dependencies">
        <img src="https://img.shields.io/librariesio/github/rwth-acis/las2peer-ThreadedComment-Service" /></a>
</p>


This is a [las2peer](https://github.com/rwth-acis/las2peer/) service allowing other services to provide comment functionality.

Other services can create a comment thread (via RMI) and set up permissions. Users post comment to this thread using a REST api.
The service makes heavy use of the distributed storage of las2peer. All user content is encrypted using UserAgents and GroupAgents
in order to be independent from specific ServiceAgents.
This means that the service can be deployed everywhere on the network while each instance uses the same data.

There is also a CommentManagementService to show how to call the CommentService's RMI methods.

ROLE widgets are provided for both services. Also, there exists a [Polymer element](https://github.com/rwth-acis/comment-thread-widget).

Features
--------

* add, edit and delete comments
* administer comments (thread owner can remove comments)
* reply to comments
* up/downvote comments
* permissions (owner = admin, writer = post comments, reply and vote, reader = read-only access)
* RMI methods to create and delete threads

Build & Run instructions
------------------------

Run ``ant build`` to build the service. Two service jars will be generated: the CommentService and the CommentExampleService (for
testing and demonstration purposes).

To test the service, run ``ant run`` and start a http server (at port 8081, otherwise you have to change the URLs in the widgets source files)
at ``/ROLE-Widget/`` and navigate to ``http://.../CommentExample/commentexample.html`` or use the provided ROLE-Widgets.

How to use in your service
--------------------------

Use the provided ``createCommentThread(long owner, long writer, long reader)`` and ``deleteCommentThread(String id)`` methods
using ``invokeServiceMethod()`` (see CommentExampleService class for an example and additional hints).

``createCommentThread()`` requires three (group) agent ids to set up permissons: ``owner`` (admin rights), ``writer`` (add comments and vote)
and ``reader`` (read-only access).
