package tutorial.storm.trident;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.tuple.Fields;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.kafka.KafkaConfig;
import storm.kafka.StringScheme;
import storm.kafka.trident.TransactionalTridentKafkaSpout;
import storm.kafka.trident.TridentKafkaConfig;
import storm.trident.TridentTopology;
import storm.trident.operation.builtin.Count;
import storm.trident.operation.builtin.FirstN;
import storm.trident.operation.builtin.MapGet;
import storm.trident.operation.builtin.TupleCollectionGet;
import storm.trident.testing.FeederBatchSpout;
import storm.trident.testing.MemoryMapState;
import tutorial.storm.trident.operations.*;
import tutorial.storm.trident.testutil.SampleTweet;
import tutorial.storm.trident.testutil.TweetIngestor;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Enno Shioji (enno.shioji@peerindex.com)
 */
public class Skeleton {
    private static final Logger log = LoggerFactory.getLogger(Skeleton.class);
    public static StormTopology buildTopology(LocalDRPC drpc, TransactionalTridentKafkaSpout spout) throws IOException {
        TridentTopology topology = new TridentTopology();
        topology
                .newStream("tweets", spout)
                .each(new Fields("str"), new Print())
        ;


        topology
                .newDRPCStream("fake", drpc)
                .each(new Fields("args"), new Print())
        ;

        return topology.build();
    }


    public static void main(String[] args) throws Exception {
        Config conf = new Config();

        LocalDRPC drpc = new LocalDRPC();
        LocalCluster cluster = new LocalCluster();

        String testKafkaBrokerHost;
        if (args.length == 0) {
            final String tmpFile = "/tmp/"+UUID.randomUUID();
            new File(tmpFile).deleteOnExit();
            TweetIngestor ingestor = new TweetIngestor(tmpFile, "test", 12000);
            ingestor.startAndWait();
            testKafkaBrokerHost = "127.0.0.1:12000";
        }else{
            checkArgument(args.length == 1);
            testKafkaBrokerHost = args[0];
        }

        TransactionalTridentKafkaSpout tweetSpout = tweetSpout(testKafkaBrokerHost);
        cluster.submitTopology("hackaton", conf, buildTopology(drpc, tweetSpout));


        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(3000);
//            System.out.println(drpc.execute("fake", "test"));
        }
    }

    private static TransactionalTridentKafkaSpout tweetSpout(String testKafkaBrokerHost) {
        KafkaConfig.BrokerHosts hosts = TridentKafkaConfig.StaticHosts.fromHostString(ImmutableList.of(testKafkaBrokerHost), 1);
        TridentKafkaConfig config = new TridentKafkaConfig(hosts, "test");
        config.scheme = new SchemeAsMultiScheme(new StringScheme());
        return new TransactionalTridentKafkaSpout(config);
    }
}
