package com.wjl.alipay.controller;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.wjl.alipay.service.PayService;
import com.wjl.common.pojo.TaotaoResult;

/**
 * 用户支付Controller
 * @author wujiale
 * 2017-11-12 下午5:01:54
 */
@Controller
public class PayController {
	
	@Autowired
	private PayService payService;
	
	private static final Logger log = LoggerFactory.getLogger(PayController.class);
	
	@RequestMapping("/pay")
	@ResponseBody
	public TaotaoResult pay(Long userId,String orderNo,HttpServletRequest request){
		System.out.println(payService.getClass());
		return payService.pay(userId, orderNo, request);
	}
	/**
	 * 前台查询支付宝返回状态
	 * @param userId
	 * @param orderNo
	 * @param request
	 * @return
	 */
	@RequestMapping("/query_pay_status")
	@ResponseBody
	public TaotaoResult queryPayStatus(Long userId,String orderNo,HttpServletRequest request){
		return payService.queryPayStatus(userId, orderNo);
	}
	
	/**
     * 支付宝后台校验数据并返回状态
     * 
     */
	@RequestMapping("/pay_callback")
	@ResponseBody
	public Object callback(HttpServletRequest request){
log.error("支付宝后台回调开始");
		Map<String,String> params = Maps.newHashMap();
        Map requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator();iter.hasNext();){
            String name = (String)iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for(int i = 0 ; i <values.length;i++){

                valueStr = (i == values.length -1)?valueStr + values[i]:valueStr + values[i]+",";
            }
            params.put(name,valueStr);
        }
log.error("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());

        //非常重要,验证回调的正确性,是不是支付宝发的.并且呢还要避免重复通知.

        params.remove("sign_type");
log.error(Configs.getSignType());
        try {
				boolean bool = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
				if (!bool) {
					return TaotaoResult.build(500, "非法请求,再闹我就报警了!");
				}
			} catch (AlipayApiException e) {
log.error("支付宝验证回调异常",e);
				e.printStackTrace();
			}
		TaotaoResult taotaoResult = payService.callback(params);
		if (taotaoResult.getData().equals("success")) {
			return "success";
		}
		return "failed";
	}
}
