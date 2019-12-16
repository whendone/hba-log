package cn.hba.syslog.consumer;import cn.hba.syslog.service.*;import cn.hutool.core.thread.ThreadFactoryBuilder;import cn.hutool.core.util.StrUtil;import cn.hutool.json.JSONUtil;import lombok.extern.slf4j.Slf4j;import org.apache.flink.api.common.functions.MapFunction;import org.apache.flink.api.common.serialization.SimpleStringSchema;import org.apache.flink.streaming.api.TimeCharacteristic;import org.apache.flink.streaming.api.datastream.DataStream;import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import javax.annotation.PostConstruct;import java.util.List;import java.util.Properties;import java.util.concurrent.*;import java.util.stream.Collectors;import java.util.stream.Stream;/** * syslog 消费者 * * @author wbw * @date 2019/11/6 10:50 */@Component@Slf4jpublic class SyslogConsumer {    @Autowired    private LogAttack logAttack;    @Autowired    private LogFlow logFlow;    @Autowired    private LogHardware logHardware;    @Autowired    private LogMenace logMenace;    @Autowired    private LogNetwork logNetwork;    @Autowired    private LogOpconf logOpconf;    @Autowired    private LogOperation logOperation;    @Autowired    private LogOther logOther;    @Autowired    private LogSecurity logSecurity;    @Autowired    private LogStrategy logStrategy;    @Autowired    private LogSysrun logSysrun;    @Value("${kafka.zookeeper.hosts}")    private String zookeeperHosts;    @Value("${kafka.hosts}")    private String kafkaHosts;    private ThreadFactory namedThreadFactory = ThreadFactoryBuilder.create().setNamePrefix(SyslogConsumer.class.getName()).build();    private ExecutorService pool = new ThreadPoolExecutor(8, 8,            3000L, TimeUnit.MILLISECONDS,            new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());    @PostConstruct    public void init() {        Properties props = this.getProps();        LogAttack attack = logAttack;        initBase(Stream.of("attack", "sysrun").collect(Collectors.toList()), attack, props);    }    /**     * 初始化 攻击 topic     */    private static void initBase(List<String> topic, LogBase base, Properties props) {        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();        // 非常关键，一定要设置启动检查点！！        env.enableCheckpointing(5000);        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);        DataStream<String> stream = env.addSource(new FlinkKafkaConsumer010<>(topic, new SimpleStringSchema(), props));        stream.rebalance().map((MapFunction<String, Object>) value -> {            System.out.println(value);//            if (JSONUtil.isJsonObj(value)) {//                value = StrUtil.format("[{}]", JSONUtil.parseObj(value).toString());//            }//            base.stream(JSONUtil.parseArray(value));            return null;        });        try {            env.execute();        } catch (Exception e) {            log.error("flink", e);        }    }    /**     * 获取 基本配置     *     * @return Properties     */    private Properties getProps() {        Properties props = new Properties();        props.setProperty("zookeeper.connect", zookeeperHosts);        props.setProperty("bootstrap.servers", kafkaHosts);        props.setProperty("group.id", "syslog");        return props;    }}