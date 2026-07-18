package com.threadia.backend.platform.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_admin_bootstrap_locks")
public class PlatformAdminBootstrapLock {

	public static final Integer SINGLETON_ID = 1;

	@Id
	private Integer id;

	protected PlatformAdminBootstrapLock() {
	}

	public PlatformAdminBootstrapLock(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}
}
