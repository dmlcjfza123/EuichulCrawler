package com.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j// log 사용을 위한 lombok 어노테이션 
@Configuration//Spring Batch의 모든 Job은 configuration으로 등록해서 사용.
public class JobConfig {

	@Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    //Job 하나 당 하나의 배치 작업 => step 을 순차적으로 수행시킴.
    //Job 객체는 여러 Step 인스턴스를 포함하는 컨테이너
    @Bean
    public Job job() {
    	//simple-job 이란 이름의 Batch Job을 생성한다.
    	//job의 이름은 별도로 지정하지 않고, 이처럼 Builder를 통해 지정한다.
        return jobBuilderFactory.get("simple-job")
                .start(step())
                //.start(simpleStep1())
                .build();
    }

    ///////////////////////////////////////////////////////////////////////////
    
    //Step 안에 Tasklet 혹은 Reader & Processor & Writer 묶음이 존재.
    //Tasklet 하나와 Reader & Processor & Writer 한 묶음이 같은 레벨
    //(Reader & Processor 가 끝나고 Tasklet으로 마무리 지을 수 없음!!)
    
    //version1. tasklet 미사용
    //Step : 실질적인 배치 처리를 정의하고 제어 하는데 필요한 모든 정보가 있는 도메인 객체
    @Bean
    //@JobScope
    public Step step() {
    	//Step 객체 생성
    	//simple-step 이란 이름의 Batch Step을 생성한다.
        return stepBuilderFactory.get("simple-step")
        		//chunk :  각 커밋 사이에 처리되는 row 수
        		//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
        		//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
        		//10개씩 묶어서 처리한다.
                .<String, StringWrapper>chunk(10)
                .reader(itemReader()) // 데이터 읽기
                .processor(itemProcess()) // 읽은 데이터 처리
                .writer(itemWriter()) // 처리한 데이터 DB에 저장
                .build();
    }
    
    public class Chunk{
    	public void chunk() {
    		A a = new A();
    		B b = new B();
    		C c = new C();
    		
    		String data = a.a();
    		StringWrapper outputData = b.b(data);
    		c.c(outputData);
    	}
    }
    
    public class A{
    	public String a() {
    		return "aa";
    	}
    }
    
    public class B{
    	public StringWrapper b(String item) {
    		return new StringWrapper(item);
    	}
    }
    
    public class C{
    	public void c(StringWrapper item) {
    		System.out.println(item);
    	}
    }
    
    //version2. tasklet 사용
    //Tasklet : 각 Step에서 수행되는 로직 (Spring Batch에서 미리 정의해놓은 Reader, Processor, Writer Interface 사용가능)
    //Tasklet은 Step 안에서 단일로 수행될 커스텀한 기능들을 선언할 때 사용한다.
    // chunk (reader + processor + writer) 를 사용하면 읽기 -> 가공하기 -> 쓰기의 반복인데 chunk 없이 한 번만 실행하도록 하는 것도 가능하다.
    @Bean
    public Step simpleStep1() {
        return stepBuilderFactory.get("simpleStep1")
                .tasklet((contribution, chunkContext) -> {
                	//Batch가 수행되면 log.info(">>> This is step1")가 출력된다.
                	System.out.println("에베베베ㅔ");
                    //log.info(">>>>> This is Step1");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
	
    //@JobScope : Job이 실행될때 생성시킨다.
    //@StepScope : Step이 실행될때 생성시킨다.
    //잡이 실행될때는 내부적으로 jobExecutionContext 를 생성 하는데, 버전(날짜시간개념)이 jobExecutionContext 정보에 들어있다. Scope를 주지않으면, 갱신이 안되서 이미 실행된 job으로 판단하기때문에, 잡실행을 아예 하지않으므로, Scope 를 둘중에 아무거나 줘야한다. 
    //Step의 ItemReader는 ArrayList에 100개의 String value를 담고 있다. (읽기)
    @Bean
//    @StepScope
    @JobScope
    public ItemReader<String> itemReader() {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
        	//System.out.println("걸림");
            list.add("test" + i);
        }

        return new ListItemReader<String>(list);
    }

    //ItemProcessor는 ItemReader에서 반환된 String List를 StringWrapper 클래스로 wrapping 한다. (가공)
//    private ItemProcessor<String, StringWrapper> itemProcess() {
//        return StringWrapper::new;
//    }
    
    @Bean
//    @StepScope
    @JobScope
    public ItemProcessor<String, StringWrapper> itemProcess(){
    	return new ItemProcessor<String, StringWrapper>(){

			@Override
			public StringWrapper process(String item) throws Exception {
				return new StringWrapper(item);
			}
    		
    	};
    }
    
    public class SimpleProcessor implements ItemProcessor<String, StringWrapper>{

		@Override
		public StringWrapper process(String item) throws Exception {
			return new StringWrapper(item);
		}
    	
    }
    
    //ItemWriter는 ItemProcessor를 통해 StringWrapper로 반환된 List를 System.out.println으로 로그를 찍는다. (쓰기)
//    private ItemWriter<StringWrapper> itemWriter() {
//        return System.out::println;
//    }
    @Bean
//    @StepScope
    @JobScope
    public ItemWriter<StringWrapper> itemWriter(){
    	return new ItemWriter<>() {

			@Override
			public void write(List<? extends StringWrapper> items) throws Exception {
//				for(StringWrapper wrapper : items) {
//					System.out.println(wrapper.value);
//				}
				System.out.println(items);
			}
    		
    	};
    }

    private class StringWrapper {
        private String value;

        StringWrapper(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.format("i'm %s", getValue());
        }
    }
}
