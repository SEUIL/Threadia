package com.threadia.backend.directmessage.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "direct_message_rooms", uniqueConstraints = @UniqueConstraint(
		name = "uq_dmr_company_users", columnNames = {"company_id", "user1_id", "user2_id"}))
public class DirectMessageRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dmr_company"))
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user1_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dmr_user1"))
	private User user1;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user2_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dmr_user2"))
	private User user2;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected DirectMessageRoom() {
	}

	public DirectMessageRoom(Company company, User user1, User user2, LocalDateTime createdAt) {
		this.company = company;
		this.user1 = user1;
		this.user2 = user2;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Company getCompany() { return company; }
	public User getUser1() { return user1; }
	public User getUser2() { return user2; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public boolean includes(Long userId) { return user1.getId().equals(userId) || user2.getId().equals(userId); }
	public User otherUser(Long userId) { return user1.getId().equals(userId) ? user2 : user1; }
}
