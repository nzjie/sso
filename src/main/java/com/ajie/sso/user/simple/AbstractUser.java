package com.ajie.sso.user.simple;

import java.util.Date;
import java.util.List;

import com.ajie.chilli.support.AbstractOuterIdSupport;
import com.ajie.chilli.support.service.ServiceSupport;
import com.ajie.dao.pojo.TbUser;
import com.ajie.sso.navigator.Menu;
import com.ajie.sso.user.Role;
import com.ajie.sso.user.User;
import com.ajie.sso.user.UserServiceExt;
import com.ajie.sso.user.enums.SexEnum;
import com.ajie.sso.user.exception.UserException;

/**
 * 用户抽象实现<br>
 * 一些get方法如果子类没有输出，返回null不要抛异常，否则转换成json会失败的
 * 
 * @author ajie
 *
 */
public abstract class AbstractUser extends ServiceSupport<TbUser, UserServiceExt> implements User {

	public AbstractUser(UserServiceExt serviceExt) {
		super(serviceExt);
	}

	/**
	 * 唯一id
	 */
	protected int id;

	/**
	 * 外部ID
	 */
	protected String outerId;

	/**
	 * 用户名
	 */
	protected String name;

	/**
	 * 邮箱
	 */
	protected String email;

	/**
	 * 创建时间
	 */
	protected Date createTime;

	/**
	 * 最后活跃时间
	 */
	protected Date lastActive;

	/**
	 * 登录token
	 */
	protected String token;

	/** 外部id实例 */
	protected OuterId outerIDInstance;

	/**
	 * 用户拥有的权限
	 */
	protected List<Role> roles;

	@Override
	public int getId() {

		return id;
	}

	protected void setOuterId(String outerId) {
		this.outerId = outerId;
	}

	public String getOuterId() {
		OuterId outerIdInstance = getOuterIdInstance();
		return outerIdInstance.getOuterId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getNickName() {
		return null;
	}

	@Override
	public String getSynopsis() {
		return null;
	}

	@Override
	public SexEnum getSex() {
		return null;
	}

	@Override
	public String getPhone() {
		return null;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public Date getCreateTime() {
		return createTime;
	}

	@Override
	public Date getLastActive() {
		return lastActive;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public void lockUser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unLockUser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLockUser() {
		return false;
	}

	@Override
	public boolean isRegisterVerification() {
		return false;
	}

	@Override
	public synchronized void addRole(Role role) {
		if (null == role) {
			return;
		}
		List<Role> roles = getRoles();
		roles.add(role);
	}

	@Override
	public void removeRole(Role role) {
		if (null == role) {
			return;
		}
		List<Role> roles = getRoles();
		roles.remove(role);
	}

	@Override
	public void removeAllRole() {
		List<Role> r = getRoles();
		r.clear();
	}

	@Override
	public boolean isContainRole(int roleId) {
		List<Role> roles = getRoles();
		for (Role r : roles) {
			if (r.getId() == roleId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isContainRole(Role role) {
		if (null == role) {
			return false;
		}
		return isContainRole(role.getId());
	}

	@Override
	public boolean isAdmin() {
		List<Role> roles = getRoles();
		for (Role r : roles) {
			if (r.getId() == Role.ROLE_SU || r.getId() == Role.ROLE_ADMIN) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setNickName(String nickName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSex(SexEnum sex) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setPhone(String identifycode, String phone) throws UserException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSynopsis(String synopsis) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setAdmin(User operator) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setHeader(String header) {
		throw new UnsupportedOperationException();

	}

	@Override
	public String getHeader() {
		return null;
	}

	@Override
	public abstract boolean vertifyPassword(String password) throws UserException;

	@Override
	public void updateLastActive() {
		this.lastActive = new Date();

	}

	@Override
	public void changePassword(String oldPassword, String newPassword) throws UserException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}

	public abstract List<Role> getRoles();

	public abstract void setRoles(List<Role> roles);

	@Override
	public boolean checkRole(Role role) {
		if (null == role) {
			return false;
		}
		return checkRole(role.getId());
	}

	@Override
	public boolean checkRole(int roleId) {
		List<Role> roles = getRoles();
		for (Role r : roles) {
			if (null == r) {
				return false;
			}
			if (roleId == r.getId()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean checkRoleForUrl(String url) {
		if (null == url) {
			return false;
		}
		List<Role> rs = getRoles();
		UserServiceExt service = getService();
		Menu menu = service.getMenuByUri(url);
		for (Role r : rs) {
			List<Menu> m = r.getMenus();
			if (m.contains(menu)) {
				return true;
			}
		}
		return false;
	}

	public OuterId getOuterIdInstance() {
		if (null == outerIDInstance) {
			outerIDInstance = new OuterId();
		}
		return outerIDInstance;
	}

	/**
	 * 内部类实现外部ID支持，因为不能多继承，所以只能使用内部类实现了<br>
	 * <p>
	 * 注： 内部类继承一般需要重写getClassType()方法，不然getClassType()方法返回的是内部类的事例对象名
	 * </p>
	 * 
	 * @author niezhenjie
	 *
	 */
	protected class OuterId extends AbstractOuterIdSupport {

		@Override
		protected String getRealId() {
			return String.valueOf(AbstractUser.this.id);
		}

		@Override
		protected String getEntityOuterId() {
			return AbstractUser.this.outerId;
		}

		@Override
		protected void setOuterId(String outerId) {
			AbstractUser.this.outerId = outerId;
		}

		@Override
		protected String getClassType() {
			// 返回最终AbstractUser的子类
			return AbstractUser.this.getClass().getSimpleName();
			// return AbstractUser.class.getSimpleName(); //返回AbstractUser
			// return getClass().getSimpleName(); //返回的是OuterId事例
		}

	}

}
