package pl.com.sages.hadoop.mapreduce;

import pl.com.sages.hadoop.mapreduce.avro.HouseKey;
import pl.com.sages.hadoop.mapreduce.avro.HouseValue;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * House Stats Avro
 */
public class HousesStatsAvro extends Configured implements Tool {

    public static class Map extends Mapper<LongWritable, Text, AvroKey<HouseKey>, AvroValue<HouseValue>> {
        public static final int HOOD_COLUMN = 1;
        public static final int TYPE_COLUMN = 2;
        public static final int LAND_AREA_COLUMN = 14;
        public static final int GROSS_AREA_COLUMN = 15;
        public static final int YEAR_COLUMN = 16;
        public static final int PRICE_COLUMN = 19;

        public static final String NON_DIGIT_REGEX = "[^\\d]";
        public static final String EMPTY_STRING_REGEX = "";

        private HouseKey houseKey = new HouseKey();
        private HouseValue houseValue = new HouseValue();

        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] dataLine = value.toString().split(",");

            houseKey.setHood(dataLine[HOOD_COLUMN]);
            houseKey.setType(dataLine[TYPE_COLUMN]);

            //houseValue.setCount(new Long(1));
            houseValue.setCount(1L);

            houseValue.setLandArea(Integer.parseInt(dataLine[LAND_AREA_COLUMN].replaceAll(NON_DIGIT_REGEX, EMPTY_STRING_REGEX)));
            houseValue.setGrossArea(Integer.parseInt(dataLine[GROSS_AREA_COLUMN].replaceAll(NON_DIGIT_REGEX, EMPTY_STRING_REGEX)));
            houseValue.setYearBuilt(Integer.parseInt(dataLine[YEAR_COLUMN].replaceAll(NON_DIGIT_REGEX, EMPTY_STRING_REGEX)));
            houseValue.setSalePrice(Integer.parseInt(dataLine[PRICE_COLUMN].replace("$", EMPTY_STRING_REGEX).replaceAll(NON_DIGIT_REGEX, EMPTY_STRING_REGEX)));

            context.write(new AvroKey<HouseKey>(houseKey),
                          new AvroValue<HouseValue>(houseValue));
        }
    }

    public static class Reduce extends Reducer<AvroKey<HouseKey>, AvroValue<HouseValue>, Text, NullWritable> {
        private Text result = new Text();

        public void reduce (AvroKey<HouseKey> avroKey, Iterable<AvroValue<HouseValue>> avroValues, Context context)
                throws IOException, InterruptedException {
            HouseKey key = avroKey.datum();

            long count = 0;
            float grossArea = 0;
            float landArea = 0;
            long year = 0;
            float price = 0;
            for (AvroValue<HouseValue> v : avroValues) {
                HouseValue value = v.datum();
                count += value.getCount();
                grossArea += value.getGrossArea();
                landArea += value.getLandArea();
                year += value.getYearBuilt();
                price += value.getSalePrice();
            }
            result.set(String.format("%s\t%s\t%d\t%.2f\t%.2f\t%d\t%.2f",
                    key.getType(), key.getHood(),
                    count, grossArea / count, landArea / count, year / count, price / count));
            context.write(result, NullWritable.get());
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = this.getConf();

        Job job = Job.getInstance(conf, "houses-stats-avro");
        job.setJarByClass(HousesStatsAvro.class);
        job.setMapperClass(HousesStatsAvro.Map.class);
        job.setReducerClass(HousesStatsAvro.Reduce.class);

        // Specify key / value
        AvroJob.setMapOutputKeySchema(job, HouseKey.getClassSchema());
        AvroJob.setMapOutputValueSchema(job, HouseValue.getClassSchema());
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // Input
        FileInputFormat.addInputPath(job, new Path(args[0]));
        job.setInputFormatClass(TextInputFormat.class);

        // Output
        String timeStamp = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        String outDir = String.format("%s/%s/%s", args[1], job.getJobName(), timeStamp);
        FileOutputFormat.setOutputPath(job, new Path(outDir));
        job.setOutputFormatClass(TextOutputFormat.class);

        // Execute job and return status
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new HousesStatsAvro(), args);
        System.exit(res);
    }
}
