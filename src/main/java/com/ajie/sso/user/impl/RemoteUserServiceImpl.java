package com.ajie.sso.user.impl;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajie.chilli.common.ResponseResult;
import com.ajie.chilli.common.enums.SexEnum;
import com.ajie.chilli.utils.HttpClientUtil;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.dao.pojo.TbUser;
import com.ajie.dao.redis.JedisException;
import com.ajie.dao.redis.RedisClient;
import com.ajie.sso.role.Role;
import com.ajie.sso.user.UserService;
import com.ajie.sso.user.exception.UserException;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 用户模块远程接口实现，对应的方法调用要与sso系统的控制器方法一致
 *
 * @author niezhenjie
 *
 */

public class RemoteUserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(RemoteUserServiceImpl.class);

	/** 单点登录系统链接 */
	private String ssohost;

	/**
	 * redis客户端服务
	 */
	@Resource
	protected RedisClient redisClient;

	public RemoteUserServiceImpl(String ssohost) {
		this.ssohost = ssohost;
	}

	@Override
	public TbUser register(String name, String passwd, HttpServletRequest request,
			HttpServletResponse response) throws UserException {
		String url = genUrl("register");
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", name);
		params.put("password", passwd);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		assertResponse(res);
		return (TbUser) res.getData();
	}

	@Override
	public TbUser update(TbUser tbUser) throws UserException {

		String url = genUrl("update");
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", JsonUtils.toJSONString(tbUser));
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		assertResponse(res);
		return (TbUser) res.getData();

	}

	@Override
	public TbUser login(String key, String password, HttpServletRequest request,
			HttpServletResponse response) throws UserException {
		String url = genUrl("login");
		Map<String, String> params = new HashMap<String, String>();
		params.put("key", key);
		params.put("password", password);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		assertResponse(res);
		return JsonUtils.toBean((JSONObject) res.getData(), TbUser.class);
	}

	@Override
	public TbUser loginByToken(String token) throws UserException {
		String url = genUrl("loginbytoken");
		Map<String, String> params = new HashMap<String, String>();
		params.put(UserService.REQUEST_TOKEN_KEY, token);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("", e1);
			}
		}
		assertResponse(res);
		TbUser user = JsonUtils.toBean((JSONObject) res.getData(), TbUser.class);
		putintoRedis(token, user);
		return user;

	}

	@Override
	public TbUser getUserById(String id) {
		String url = genUrl("getuserbyid");
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", id);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		try {
			assertResponse(res);
		} catch (UserException e) {
		}
		TbUser user = (TbUser) res.getData();
		return user;
	}

	@Override
	public TbUser getUserByName(String name) {
		String url = genUrl("getuserbyname");
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", name);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		try {
			assertResponse(res);
		} catch (UserException e) {
		}
		return (TbUser) res.getData();
	}

	@Override
	public TbUser getUserByEmail(String email) {
		String url = genUrl("getuserbyname");
		Map<String, String> params = new HashMap<String, String>();
		params.put("email", email);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		try {
			assertResponse(res);
		} catch (UserException e) {
		}
		return (TbUser) res.getData();
	}

	@Override
	public TbUser getUserByPhone(String phone) {
		String url = genUrl("getuserbyname");
		Map<String, String> params = new HashMap<String, String>();
		params.put("phone", phone);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
		} catch (IOException e) {
			// 重试
			try {
				result = HttpClientUtil.doGet(url, params);
				res = getResponse(result);
			} catch (IOException e1) {
				logger.error("注册失败" + e1);
			}
		}
		try {
			assertResponse(res);
		} catch (UserException e) {
		}
		return (TbUser) res.getData();

	}

	@Override
	public List<TbUser> searchUsers(int state, Date registerDate, SexEnum sex) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 拼接远程链接
	 * 
	 * @param method
	 *            控制器方法名
	 * @return
	 */
	private String genUrl(String method) {
		if (!ssohost.startsWith("http")) {
			throw new IllegalArgumentException("sso系统链接错误" + ssohost);
		}
		if (!ssohost.endsWith("/")) {
			ssohost += "/";
		}
		return ssohost + method + ".do";
	}

	private ResponseResult getResponse(String result) {
		return JsonUtils.toBean(result, ResponseResult.class);
	}

	private void assertResponse(ResponseResult response) throws UserException {
		if (null == response)
			throw new UserException("网络异常，请稍后再试试");
		int code = response.getCode();
		if (ResponseResult.CODE_ERR == code)
			throw new UserException(response.getMsg());
	}

	private boolean putintoRedis(String key, TbUser user) {
		boolean b = false;
		try {
			redisClient.hset(REDIS_PREFIX, key, user);
		} catch (JedisException e) {
			logger.info("token置入redis失败" + key);
		}
		b = true;
		return b;
	}

	@Override
	public List<Role> getRoles() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean checkRole(TbUser user, String checkurl) {
		String url = genUrl("checkrole");
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", JsonUtils.toJSONString(user));
		params.put("url", checkurl);
		ResponseResult res = null;
		String result = "";
		try {
			result = HttpClientUtil.doGet(url, params);
			res = getResponse(result);
			return Boolean.valueOf(res.getMsg());
		} catch (IOException e) {
		}
		try {
			assertResponse(res);
		} catch (UserException e) {
		}
		return false;

	}
}
