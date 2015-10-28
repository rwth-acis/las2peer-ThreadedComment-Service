#!/bin/bash

# this script starts a las2peer node providing the example service of this project
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5432 -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9011 uploadStartupDirectory\(\'etc/startup\'\) startService\(\'i5.las2peer.services.commentService.CommentService\',\'someNewPass\'\) startService\(\'i5.las2peer.services.commentExampleService.CommentExampleService\',\'someNewPass\'\) startWebConnector interactive
