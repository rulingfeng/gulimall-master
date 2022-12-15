package com.vivi.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.vivi.common.constant.OrderConstant;
import com.vivi.gulimall.order.entity.OrderEntity;
import com.vivi.gulimall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author wangwei
 * 2021/1/28 10:23
 *
 * 30min未支付释放订单消息
 */
@Slf4j
@Component
@RabbitListener(queues = {OrderConstant.ORDER_RELEASE_ORDER_QUEUE})
public class OrderReleaseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void releaseOrder(OrderEntity orderEntity, Message message, Channel channel) throws IOException {
        System.out.println("收到过期订单：" + orderEntity.getOrderSn());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            orderService.closeOrder(orderEntity);
            // TODO 这里应该手动调用一次支付宝的收单接收，以往由于网络延迟，我们已撤销订单，然而用户继续支付，导致后续支付宝通知到来，我们将其又改为已支付状态
            // 消费成功，手动ack
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 消费失败，消息重新入队
            channel.basicReject(deliveryTag, true);
            log.error("消息队列手动ack失败：com.vivi.gulimall.order.listener.OrderReleaseListener.releaseOrder, Error: {}", e.getMessage());
        }
    }
}
