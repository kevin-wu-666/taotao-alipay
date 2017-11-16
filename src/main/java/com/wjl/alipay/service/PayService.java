package com.wjl.alipay.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.wjl.common.pojo.TaotaoResult;

public interface PayService {
	TaotaoResult pay(Long userId,String orderNo,HttpServletRequest request);
	TaotaoResult callback(Map<String, String> map);
	TaotaoResult queryPayStatus(Long userId, String orderNo);
}
