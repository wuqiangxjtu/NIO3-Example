package cn.ryanwu.nio2.exp.reactor.mutiple;

import java.io.IOException;

import cn.ryanwu.nio2.exp.utils.SystemUtil;


public class NIOReactorPool {
	
	private final NIOReactor[] reactors;
	private volatile int nextReactor;

	public NIOReactorPool(String name, int poolSize) throws IOException {
		reactors = new NIOReactor[poolSize];
		SystemUtil.println("Begin to initilize reactor pool ... ");
		for (int i = 0; i < poolSize; i++) {
			NIOReactor reactor = new NIOReactor(name + "-" + i);
			reactors[i] = reactor;
			new Thread(reactor).start();
			SystemUtil.println("Reactor thread " + i + " start.");
		}
	}

	public NIOReactor getNextReactor() {
		if (++nextReactor == reactors.length) {
			nextReactor = 0;
		}
		return reactors[nextReactor];
	}

}
