#!/bin/bash

# this script starts a las2peer node providing the example service of this project
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -s service -p 9011 --node-id-seed 1 uploadStartupDirectory\(\'etc/startup\'\) startService\(\'i5.las2peer.services.threadedCommentService.ThreadedCommentService@0.2.0\'\) startService\(\'i5.las2peer.services.commentManagementService.CommentManagementService@0.2.0\'\) startWebConnector interactive
