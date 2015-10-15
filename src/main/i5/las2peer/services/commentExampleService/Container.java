package i5.las2peer.services.commentExampleService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Container implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private List<String> threads;
	
	public Container() {
		this.threads = new ArrayList<>();
	}
	
	public List<String> getThreads() {
		return this.threads;
	}
}
