package com.ajie.sso.navigator;

import java.util.List;

import com.ajie.sso.user.User;

/**
 * 导航条管理器
 * 
 * @author niezhenjie
 */
public interface NavigatorMgr {

	/**
	 * 根据uri返回所属菜单
	 * 
	 * @param uri
	 * @return
	 */
	Menu getMenuByUri(String uri);

	/**
	 * 根据登录用户，返回的菜单
	 * 
	 * @param user
	 * @return
	 */
	List<Menu> getMenus(User user);

	List<Menu> getMenus();

	public Menu getMenuById(int id);
}