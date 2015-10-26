/**
 * Comment Widget to display a comment thread (and to interact with it).
 */
var api = i5.las2peer.jsAPI;

/*
  Check explicitly if gadgets is known, i.e. script is executed in widget environment.
  Allows compatibility as a Web page.
 */

var widget = false;

if (typeof gadgets !== "undefined" && gadgets !== null) {
  iwcCallback = function (intent) {
    if (intent.action === "SHOW_COMMENTS") {
    }
  };
  iwcManager = new api.IWCManager(iwcCallback);

  widget = true;
}

// login
var login = new api.Login(api.LoginTypes.HTTP_BASIC);
login.setUserAndPassword("alice", "pwalice"); // TODO get login from somewhere

var requestSender = new api.RequestSender("http://localhost:8080/commentexample", login);


$(document).ready(function () {
  init();
});

var init = function () {
  showThreads();
};

var renderThread = function(id) {
  var html='<li id="thread-'+id+'">';

  if (widget) {
    html+='<a href="#" onclick="sendIntent(\''+id+'\');return false;">'+id+'</a>';
  }
  else {
    html+='<a href="../Comment/comments.html#'+id+'" target="_blank">'+id+'</a>';
  }

  html += ' (<button onclick="deleteThread(\''+id+'\')">delete</button>)';

  html+='</li>';

  return html;
}

var showThreads = function () {
	var request = new api.Request("get", "threads", "", function (data) {
    data = JSON.parse(data);

		var html = '<ul id="threads">';
    for (var i=0;i<data.length;i++) {
      html+=renderThread(data[i]);
    }
		html += '</ul>';

    html+='<div class="add"><form onsubmit="submitAddForm(this);return false;">'
    +'Owner: <input type="text" name="owner" value="1741926561073830633" /><br />'
    +'Writer: <input type="text" name="writer" value="-2932758749278370327" /><br />'
    +'Reader: <input type="text" name="reader" value="1424191584300014900" /><br />'
    +'<input type="submit" value="Create Thread" /></form></div>';


		document.getElementById('main').innerHTML = html;
	});
	requestSender.sendRequestObj(request);
};

var addThread = function (owner,writer,reader) {
	var request = new api.Request("post", "threads", "{owner:"+owner+", writer:"+writer+", reader:"+reader+"}", function (data) {
		document.getElementById("threads").innerHTML+=renderThread(data);
		
		if (widget)
			sendIntent(data);
	});
	requestSender.sendRequestObj(request);
}

var deleteThread = function (threadId) {
	var request = new api.Request("delete", "threads/"+threadId, "", function (data) {
		document.getElementById('thread-'+threadId).parentElement.removeChild(document.getElementById('thread-'+threadId));
		
		if (widget)
			sendIntent("");
	});
	requestSender.sendRequestObj(request);
}

var submitAddForm = function(target) {
	addThread(target.elements[0].value,target.elements[1].value,target.elements[2].value);
	target.reset();
}

var sendIntent = function(id) {
  iwcManager.sendIntent("SHOW_COMMENTS", id);
}
