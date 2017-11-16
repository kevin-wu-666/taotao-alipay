package com.wjl.alipay.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.wjl.common.pojo.TaotaoResult;
import com.wjl.mapper.TbOrderItemMapper;
import com.wjl.mapper.TbOrderMapper;
import com.wjl.pojo.TbOrder;
import com.wjl.pojo.TbOrderExample;
import com.wjl.pojo.TbOrderItem;
import com.wjl.pojo.TbOrderItemExample;

/**
 * 用户支付service
 * @author wujiale
 * 2017-11-12 下午5:14:43
 */

@Service
public class PayServiceImpl implements PayService{
	@Autowired
	private TbOrderMapper tbOrderMapper;
	@Autowired
	private TbOrderItemMapper tbOrderItemMapper;
	
	public void setTbOrderMapper(TbOrderMapper tbOrderMapper) {
		this.tbOrderMapper = tbOrderMapper;
	}

	public void setTbOrderItemMapper(TbOrderItemMapper tbOrderItemMapper) {
		this.tbOrderItemMapper = tbOrderItemMapper;
	}

	// 支付宝当面付2.0服务
    private static AlipayTradeService   tradeService;
    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);

	@Override
	public TaotaoResult pay(Long userId, String orderNo,HttpServletRequest request) {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		log.info(stackTrace[1].getMethodName());
System.out.println(stackTrace[1].getMethodName());
		Map<String, String> map = new HashMap<>();
		TbOrderExample example = new TbOrderExample();
		example.createCriteria().andOrderIdEqualTo(orderNo).andUserIdEqualTo(userId);
		List<TbOrder> list = tbOrderMapper.selectByExample(example );
		if (list == null || list.size() <= 0) {
			return TaotaoResult.build(500, "查询不到该订单,订单有误");
		}
		TbOrder order = list.get(0);
		
		// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = orderNo;

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "淘淘商城扫码支付,订单号:"+orderNo;

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder("淘淘商城购买商品共消费:").append(totalAmount).toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        TbOrderItemExample example1 = new TbOrderItemExample();
        example1.createCriteria().andOrderIdEqualTo(orderNo);
		List<TbOrderItem> orderItems = tbOrderItemMapper.selectByExample(example1 );
		for (TbOrderItem tbOrderItem : orderItems) {
			// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
			GoodsDetail goods = GoodsDetail.newInstance(tbOrderItem.getItemId(), tbOrderItem.getTitle(), tbOrderItem.getPrice(), tbOrderItem.getNum());
			// 创建好一个商品后添加至商品明细列表
			goodsDetailList.add(goods);
		}

        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
		
        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
            .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
            .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
            .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
            .setTimeoutExpress(timeoutExpress)
            .setNotifyUrl("http://9d591301.ngrok.io/pay/pay_callback")//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
            .setGoodsDetailList(goodsDetailList);
        
        Configs.init("zfbinfo.properties");
        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);
                String string = request.getRealPath("/QrCode");
                File path = new File(string);
                if (!path.exists()) {
					path.mkdirs();
				}
                // 需要修改为运行机器上的路径
                String filePath = String.format(path+"\\qr-%s.png",response.getOutTradeNo());
//                String fileName = String.format("/qr-%s.png",response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);
//                File file = new File(filePath);
//                FileUtils.copyFileToDirectory(file, filePath);
log.info("filePath:" + filePath);
                map.put("qrCodeUrl", filePath);
                return TaotaoResult.ok(map);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                return TaotaoResult.build(500, "支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return TaotaoResult.build(500, "系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return TaotaoResult.build(500, "不支持的交易状态，交易返回异常!!!");
        }
	}
	
	// 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                    response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }
    
    /**
     * 支付宝后台校验数据并返回状态
     * 
     */
    public TaotaoResult callback(Map<String, String> map){
log.error("-------service支付宝进入回调");
    	String orderNo = map.get("out_trade_no");
    	String trade_no = map.get("trade_no");
    	String trade_status = map.get("trade_status");
log.error(trade_status);    	
    	TbOrder order = tbOrderMapper.selectByPrimaryKey(orderNo);
    	if (order == null) {
			return TaotaoResult.build(500, "非本商城的订单");
		}
    	if (!(order.getStatus().equals(1)||order.getStatus().equals(6))) {
log.error("支付宝重复调用,返回success停止调用");
			return TaotaoResult.ok("success");
		}
    	if (trade_status.equals("TRADE_SUCCESS")) {
    		DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(map.get("gmt_payment"));
    		order.setPaymentTime(dateTime.toDate());
			order.setStatus(2);
			tbOrderMapper.updateByPrimaryKeySelective(order);
		}
log.error("订单号:"+orderNo+"交易成功,支付宝交易号:"+trade_no);
    	return TaotaoResult.ok("success");
    }
    
    /**
     * 前台查询支付宝返回状态
     * 
     */
    public TaotaoResult queryPayStatus(Long userId, String orderNo){
    	TbOrderExample example = new TbOrderExample();
		example.createCriteria().andOrderIdEqualTo(orderNo).andUserIdEqualTo(userId);
		List<TbOrder> list = tbOrderMapper.selectByExample(example );
		log.info("根据id和no查询订单");
		if (list == null || list.size() <= 0) {
			return TaotaoResult.build(500, "查询不到该订单,订单有误");
		}
		TbOrder order = list.get(0);
		if (!(order.getStatus().equals(1)||order.getStatus().equals(6))) {
			return TaotaoResult.ok("success");
		}
		return TaotaoResult.build(500, "未知错误");
    }

}
