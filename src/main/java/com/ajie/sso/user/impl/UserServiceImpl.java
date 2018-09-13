package com.ajie.sso.user.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ajie.chilli.Collection.simple.SwitchUnmodifiableList;
import com.ajie.chilli.support.OuterIdException;
import com.ajie.chilli.support.OuterIdUtil;
import com.ajie.chilli.utils.Toolkits;
import com.ajie.chilli.utils.XmlHelper;
import com.ajie.chilli.utils.common.ConstantPool;
import com.ajie.chilli.utils.common.StringUtil;
import com.ajie.dao.mapper.TbLabelMapper;
import com.ajie.dao.mapper.TbUserMapper;
import com.ajie.dao.pojo.TbUser;
import com.ajie.dao.pojo.TbUserExample;
import com.ajie.dao.redis.JedisException;
import com.ajie.dao.redis.RedisClient;
import com.ajie.sso.navigator.Menu;
import com.ajie.sso.navigator.NavigatorMgr;
import com.ajie.sso.user.Role;
import com.ajie.sso.user.User;
import com.ajie.sso.user.UserService;
import com.ajie.sso.user.UserServiceExt;
import com.ajie.sso.user.exception.UserException;
import com.ajie.sso.user.simple.SimpleRole;
import com.ajie.sso.user.simple.SimpleUser;
import com.ajie.sso.user.simple.XmlUser;

/**
 * 用户服务实现
 * 
 * @author niezhenjie
 */
@Service
public class UserServiceImpl implements UserService, UserServiceExt, InitializingBean {
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	/**
	 * 配置用户文件路径
	 */
	@Value("${xmluser_path_name}")
	protected String xmlUserPath;

	/**
	 * 权限表文件路径
	 */
	@Value("${role_file__path_name}")
	protected String rolePath;

	/**
	 * 锁
	 */
	Object lock = new Object();

	/**
	 * 导航服务
	 */
	@Resource
	protected NavigatorMgr navigatorService;

	/**
	 * redis客户端服务
	 */
	@Resource
	protected RedisClient redisClient;

	/**
	 * 标签数据库映射
	 */
	@Resource
	protected TbLabelMapper labelMapper;

	/**
	 * 用户数据库映射
	 */
	@Resource
	protected TbUserMapper userMapper;

	/**
	 * 配置用户，初始化完成后需要转换成只读列表
	 */
	protected final SwitchUnmodifiableList<User> xmlUserCache;

	/**
	 * 所有的权限列表，初始化完成后需要转换成只读列表
	 */
	protected final SwitchUnmodifiableList<Role> roleDatas;

	public UserServiceImpl() {
		xmlUserCache = new SwitchUnmodifiableList<User>();
		roleDatas = new SwitchUnmodifiableList<Role>();
	}

	public List<User> getXmlUsers() {
		return xmlUserCache;
	}

	@Override
	public User register(TbUser user) throws UserException {
		if (null == user) {
			throw new UserException("参数有误");
		}
		User u = new SimpleUser(this, user.getName(), user.getPassword()); // 构造时会初始化一些属性
		int i = userMapper.insert(u.toPojo());
		if (i > 0) {
			return u;
		}
		return null;
	}

	@Override
	public List<User> getUsers(int page) {
		// TODO
		return null;
	}

	@Override
	public boolean checkUserExit(String name) throws UserException {
		User user = getUserByName(name);
		return null == user;
	}

	@Override
	public User login(String name, String password) throws UserException {
		if (null == name) {
			throw new UserException("用户账号不存在");
		}
		if (null == password) {
			throw new UserException("密码错误");
		}
		User user = getUserByName(name);
		String token = Toolkits.gen16UniqueId();
		user.setToken(token);
		try {
			redisClient.set(USER_TOKEN_PRE + token, user.toPojo());
			// 30分钟 token失效
			redisClient.expire(USER_TOKEN_PRE + token, 60 * 30);
		} catch (JedisException e) {
		}
		if (!user.vertifyPassword(password)) {
			throw new UserException("密码错误");
		}
		return user;
	}

	@Override
	public User getUserById(String outerId) throws UserException {
		int id = 0;
		try {
			String oId = OuterIdUtil.getIdFromOuterId(outerId);
			id = Integer.valueOf(oId);
			for (User user : xmlUserCache) { // xml配置用户优先
				if (null == user) {
					continue;
				}
				if (user.getId() == id) {
					return user;
				}
			}
		} catch (OuterIdException e) {
			logger.warn("无法从外部ID获取用户: " + e);
			throw new UserException("用户不存在");
		}
		TbUser tbUser = null;
		try {
			tbUser = redisClient.hgetAsBean(UserService.USER_REDIS_COOKIE_KEY, String.valueOf(id),
					TbUser.class);
		} catch (JedisException e) {
			logger.warn("", e);
		}
		if (null != tbUser) {
			return new SimpleUser(this, tbUser);
		}
		// redis没找着，尝试去数据库中查找
		tbUser = userMapper.selectByPrimaryKey(id);
		if (null == tbUser) {
			return null;
		}
		// 放入redis
		try {
			redisClient.hset(UserService.USER_REDIS_COOKIE_KEY, String.valueOf(tbUser.getId()),
					tbUser);
		} catch (JedisException e) {
			// ignore redisClient已打印日志，无需外抛
		}
		return new SimpleUser(this, tbUser);
	}

	@Override
	public User getUserByToken(String token) throws UserException {
		try {
			TbUser tbUser = redisClient.getAsBean(USER_TOKEN_PRE + token, TbUser.class);
			if (null != tbUser) {
				return new SimpleUser(this, tbUser);
			}
		} catch (JedisException e) {
			// ignore redisClient已打印日志，无需外抛
		}
		return null;
	}

	@Override
	public User getUserByName(String name) throws UserException {
		if (null == name) {
			throw new UserException("用户账号不存在");
		}
		List<User> users = this.xmlUserCache; // xml配置用户优先
		if (null != users && !users.isEmpty()) {
			for (User user : users) {
				if (null == user) {
					continue;
				}
				if (StringUtil.eq(user.getName(), name) || StringUtil.eq(user.getEmail(), name)
						|| StringUtil.eq(user.getPhone(), name)) {
					return user;
				}
			}
		}
		TbUser u = null;
		// redis根据用户名查找
		try {
			u = redisClient.hgetAsBean(UserService.USER_REDIS_COOKIE_KEY, name, TbUser.class);
		} catch (JedisException e1) {
		}
		if (null != u) {
			return new SimpleUser(this, u);
		}
		// 到数据库查找
		TbUserExample example = new TbUserExample();
		example.createCriteria().andNameEqualTo(name);
		List<TbUser> tbUsers = userMapper.selectByExample(example);
		if (null == tbUsers || tbUsers.isEmpty()) {
			throw new UserException("用户账号不存在");
		}
		if (tbUsers.size() > 1) {
			logger.warn("用户账号异常，用户名结果不唯一: " + name);
			throw new UserException("用户账号异常，请联系管理员");
		}
		u = tbUsers.get(0);
		// 放入redis
		try {
			redisClient.hset(UserService.USER_REDIS_COOKIE_KEY, name, u);
		} catch (JedisException e) {
			// ignore redisClient已打印日志，无需外抛
		}
		return new SimpleUser(this, u);

	}

	/**
	 * 加载配置用户
	 * 
	 * @param doc
	 */
	protected void load(String xmlFile) throws IOException {
		Document doc = XmlHelper.parseDocument(xmlFile);
		if (null == doc) {
			logger.error(xmlFile + "配置文件加载失败");
			return;
		}
		parseXmlUser(doc);
	}

	/**
	 * 加载配置用户
	 * 
	 * @param doc
	 */
	@SuppressWarnings("unchecked")
	protected synchronized void parseXmlUser(Document doc) {
		long start = System.currentTimeMillis();
		String setter = null;
		try {
			Element root = doc.getRootElement();
			List<Element> users = root.elements("user");
			for (Element ele : users) {
				String sid = ele.attributeValue("id");
				int id = Integer.valueOf(sid);
				String username = ele.attributeValue("name");
				String password = ele.attributeValue("password");
				User user = new XmlUser(this, id, username, password);
				// userCache.put(id, user);
				List<Element> propeties = ele.elements("property");
				// 从配置中获取属性并通过setter设置进去
				for (Element el : propeties) {
					String setterName = el.attributeValue("name");
					String value = el.attributeValue("value");
					setter = getSetter(setterName);

					if (null == value) {
						// value为空 ？ 可能多个value 只有权限有传入多个值
						List<Role> roles = new ArrayList<Role>();
						user.setRoles(roles);
						List<Element> values = el.elements("value");
						if (null != values) {
							for (Element e : values) {
								try {
									int roleId = Toolkits.Hex2Deci(e.getTextTrim());
									if (roleDatas.size() == 0) {
										break;
									}
									for (Role r : roleDatas) {
										if (null == r) {
											continue;
										}
										if (r.getId() == roleId) {
											roles.add(r);
											break;
										}
									}
								} catch (Exception e2) {
									logger.error("无效权限id: " + e.getTextTrim());
									continue;
								}
							}
						}
					} else {
						Method method = getMethod(user, setter, String.class);
						method.invoke(user, value);
					}
				}
				xmlUserCache.add(user);
			}
			xmlUserCache.swithUnmodifiable();
			long end = System.currentTimeMillis();
			logger.info("已从配置文件中加载配置用户，耗时 " + (end - start) + " ms");
		} catch (IllegalAccessException ex) {
			logger.error("反射调用setter出错setter:" + setter + " , " + Toolkits.printTrace(ex));
		} catch (IllegalArgumentException ex) {
			logger.error("反射调用setter出错 setter: " + setter + " " + Toolkits.printTrace(ex));
		} catch (InvocationTargetException ex) {
			logger.error("反射调用setter出错 setter:" + setter + " " + Toolkits.printTrace(ex));
		}
	}

	protected String getSetter(String setter) {
		int len = setter.length();
		if (null == setter || "".equals(setter) || len < 1) {
			return ConstantPool._NULLSTR;
		}
		char fl = setter.charAt(0);
		String flt = String.valueOf(fl);
		if (len == 1) {
			return "set" + flt.toUpperCase();
		}
		String lack = setter.substring(1, len);
		return "set" + flt.toUpperCase() + lack;

	}

	protected Method getMethod(Object obj, String methodName, Class<?>... paramType) {
		if (null == obj) {
			return null;
		}
		Class<? extends Object> cla = obj.getClass();
		try {
			Method method = cla.getMethod(methodName, paramType);
			return method;
		} catch (NoSuchMethodException e) {
			logger.error("setter方法不存在:" + methodName + " " + Toolkits.printTrace(e));
		} catch (SecurityException e) {
			logger.error("setter方法不存在:" + methodName + " " + Toolkits.printTrace(e));
		}
		return null;
	}

	public void loadRole(String path) throws IOException {
		Document doc = XmlHelper.parseDocument(path);
		if (null == doc) {
			logger.error(path + "配置文件加载失败");
			return;
		}
		parseRoles(doc);

	}

	@SuppressWarnings("unchecked")
	public synchronized void parseRoles(Document doc) {
		long start = System.currentTimeMillis();
		Element root = doc.getRootElement();
		List<Element> rolesEle = root.elements("role");
		for (Element ele : rolesEle) {
			String idstr = ele.attributeValue("id");
			int id = 0;
			try {
				id = Toolkits.Hex2Deci(idstr);
			} catch (Exception e) {
				logger.error("权限id格式错误，应为0x开头十六进制形式：" + idstr);
				continue;
			}

			String name = ele.attributeValue("name");
			String desc = ele.attributeValue("desc");
			Role role = new SimpleRole(id, name, desc);
			roleDatas.add(role);
			List<Menu> menus = new ArrayList<Menu>();
			role.setMenus(menus);
			if (id == Role.ROLE_SU) {
				menus = navigatorService.getMenus();
				role.setMenus(menus);// 返回新的对象，这里的menus对象的地址和上述的已经不一致了，所以需要在set一次
				continue;
			}
			Element el = ele.element("menus");
			List<Element> values = el.elements("value");
			for (Element value : values) {
				String s = value.getTextTrim();
				int menuId = -1;
				try {
					menuId = Toolkits.Hex2Deci(s);
				} catch (NumberFormatException e) {
					logger.error("无效menu id:" + s);
					continue;
				}
				Menu menu = navigatorService.getMenuById(menuId);
				menus.add(menu);
			}
		}
		roleDatas.swithUnmodifiable();
		long end = System.currentTimeMillis();

		logger.info("已从配置文件中初始化了用户数据 , 耗时 " + (end - start) + " ms");

	}

	@Override
	public List<Role> getRoles() {
		if (null == roleDatas) {
			return Collections.emptyList();
		}
		return roleDatas;
	}

	@Override
	public String getVertifycode(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Menu getMenuByUri(String uri) {
		if (StringUtil.isEmpty(uri)) {
			return null;
		}
		return navigatorService.getMenuByUri(uri);
	}

	/**
	 * 执行完构造方法和所有的setter方法后调用<br>
	 * 因为多个setter方法执行的顺序是不可预见的，所以需要在这个方法里按照顺序执行<br>
	 * 如果直接使用setter方法执行，然后注入setter方法参数，有可能会先执行load方法，此时<br>
	 * 在load里需要用到role的数据，但是role还没有初始化，所以会报错或者没有数据
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		synchronized (lock) {
			loadRole(rolePath);
			load(xmlUserPath);
		}

	}

}
