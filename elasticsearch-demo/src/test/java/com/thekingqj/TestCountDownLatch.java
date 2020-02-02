package com.thekingqj;

import java.util.concurrent.CountDownLatch;

public class TestCountDownLatch {
 
	private static CountDownLatch cld = new CountDownLatch(10);
	
	public static void main(String[] args) {
		for (int i = 0; i <10; i++) {
			Thread t = new Thread(new Runnable() {
				public void run() {

						try {
						cld.await();//将线程阻塞在此，等待所有线程都调用完start()方法，一起执行
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(Thread.currentThread().getName() + ":" + System.currentTimeMillis());

				}
			});
			t.start();
			cld.countDown();
		}
	}
	
}
