package com.threadia.backend.thread.exception;

public class ThreadNotFoundException extends RuntimeException {
	public ThreadNotFoundException(Long threadId) { super("쓰레드를 찾을 수 없습니다: " + threadId); }
}
