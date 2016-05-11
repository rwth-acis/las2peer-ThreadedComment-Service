#!/bin/bash

# this script starts a las2peer node providing the example service of this project
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9011 --node-id-seed 1 uploadStartupDirectory\(\'etc/startup\'\) startService\(\'i5.las2peer.services.commentService.CommentService@0.1\',\'someNewPass\'\) startService\(\'i5.las2peer.services.commentManagementService.CommentManagementService@0.1\',\'someNewPass\'\) startWebConnector interactive
