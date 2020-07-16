package com.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import com.dao.MonsterDao;
import com.dto.MonsterChainSkillDto;
import com.dto.MonsterDto;
import com.dto.MonsterEvolutionDto;
import com.dto.MonsterLeaderSkillDto;
import com.dto.MonsterNormalSkillDto;
import com.dto.MonsterPreviewHrefCrawlStepDto;
import com.jsoup.MonsterCrawler;
import com.step.readerProcessorWriter.MonsterChainSkillDtoCrawlerDivide;
import com.step.readerProcessorWriter.MonsterDtoCrawlerDivide;
import com.step.readerProcessorWriter.MonsterEvolutionDtoCrawlerDivide;
import com.step.readerProcessorWriter.MonsterLeaderSkillDtoCrawlerDivide;
import com.step.readerProcessorWriter.MonsterNormalSkillDtoCrawlerDivide;
import com.step.readerProcessorWriter.MonsterPreviewHrefCrawlDivide;
import com.step.tasklet.MonsterScreenshotUrlCrawlTasklet;;

@Configuration // Spring Batch의 모든 Job은 configuration으로 등록해서 사용.
public class MonsterJobConfig {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public SimpleFlow splitSixFlow() {
		Flow monsterScreenshotUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterScreenshotUrlCrawlflow")
				.start(monsterPreviewIconUrlCrawlStep())
		        .build();
		
		Flow monsterPreviewIconUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterPreviewIconUrlCrawlflow")
				.start(monsterScreenshotUrlCrawlStep())
		        .build();
		
		Flow monsterEvolutionIconUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterEvolutionIconUrlCrawlflow")
				.start(monsterEvolutionIconUrlCrawlStep())
		        .build();
		
		Flow setMonsterNormalSkillFlow = new FlowBuilder<SimpleFlow>("setMonsterNormalSkillFlow")
				.start(getObjectUrlAndsetMonsterNormalSkillDtoAndInsertDBstep())
				.build();
		
		Flow setMonsterChainSkillFlow = new FlowBuilder<SimpleFlow>("setMonsterChainSkillFlow")
				.start(getObjectUrlAndsetMonsterChainSkillDtoAndInsertDBstep())
				.build();
		
		Flow setMonsterLeaderSkillFlow = new FlowBuilder<SimpleFlow>("setMonsterLeaderSkillFlow")
				.start(getObjectUrlAndsetMonsterLeaderSkillDtoAndInsertDBstep())
				.build();
		
	    SimpleFlow splitFlow = new FlowBuilder<SimpleFlow>("SimpleSplitSixFlows")
	        .split(new SimpleAsyncTaskExecutor())
	        .add(monsterScreenshotUrlCrawlflow, monsterPreviewIconUrlCrawlflow, monsterEvolutionIconUrlCrawlflow,
	        		setMonsterNormalSkillFlow,setMonsterChainSkillFlow,setMonsterLeaderSkillFlow)
	        .build();
	    
	    return new FlowBuilder<SimpleFlow>("splitSixFlow")
	    	.start(splitFlow)
	        .end();
	}
	
	@Bean
	public SimpleFlow splitTwoFlow() {
		 Flow setMonsterFlow = new FlowBuilder<SimpleFlow>("setMonsterFlow")
				.start(getObjectUrlAndSetMonsterDtoAndInsertDBstep())
				.build();
		 
		 Flow setMonsterEvolutionFlow = new FlowBuilder<SimpleFlow>("setMonsterEvolutionFlow")
					.start(getObjectUrlAndSetMonsterEvolutionDtoAndInsertDBstep())
					.build();
	
		 SimpleFlow splitFlow = new FlowBuilder<SimpleFlow>("SimpleSplitTwoFlows")
			        .split(new SimpleAsyncTaskExecutor())
			        .add(setMonsterFlow,setMonsterEvolutionFlow)
			        .build();
		 
		 return new FlowBuilder<SimpleFlow>("splitTwoFlow")
			    	.start(splitFlow)
			        .end();
	}
	
	
	@Bean
	public Job job() {
		Flow monsterScreenshotUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterScreenshotUrlCrawlflow")
				.start(monsterPreviewIconUrlCrawlStep())
		        .build();
		
		Flow monsterPreviewIconUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterPreviewIconUrlCrawlflow")
				.start(monsterScreenshotUrlCrawlStep())
		        .build();
		
		Flow monsterEvolutionIconUrlCrawlflow = new FlowBuilder<SimpleFlow>("monsterEvolutionIconUrlCrawlflow")
				.start(monsterEvolutionIconUrlCrawlStep())
		        .build();
		
		Flow splitFlow = new FlowBuilder<SimpleFlow>("splitFlow")
	        .split(new SimpleAsyncTaskExecutor())
	        .add(monsterScreenshotUrlCrawlflow, monsterPreviewIconUrlCrawlflow, monsterEvolutionIconUrlCrawlflow)
	        .build();
		
		//version1 - jobBuilderFactory에서 . 연결로 정의하기.
//		return jobBuilderFactory.get("monster-job")
//				.start(monsterPreviewIconUrlCrawlflow)
//				.split(new SimpleAsyncTaskExecutor())
//				.add(monsterScreenshotUrlCrawlflow,monsterEvolutionIconUrlCrawlflow)
//				.end()
//				.build();
		
		//version2 - job 에 flow 선언해두고 start로 한번에 하기.
//		return jobBuilderFactory.get("monster-job")
//				.start(splitFlow)
//				.end()
//				.build();
		
		//version3 - job 밖에 flow 만들어두고 함수호출해서 flow 받아 start 하기.
		return jobBuilderFactory.get("monster-job")
				.start(monsterPreviewHrefCrawlStep()) //기본적으로 스텝은 자신의 BatchStatus에 대응되는 ExitStatus값을 가진다. BatchStatus는 완료,시작중,시작됨,중지중,중지됨,실패,포기,알수없음 중 하나를 가진다. https://books.google.co.kr/books?id=AUJvDwAAQBAJ&pg=PA600&lpg=PA600&dq=spring+batch+.on(%22failed%22)&source=bl&ots=zvvtjGEqVp&sig=ACfU3U0wBN4Gqyp_F1hSxiNS-YhTeVFfnA&hl=ko&sa=X&ved=2ahUKEwi1t6OX98TpAhUHhZQKHYhYDVYQ6AEwBnoECAsQAQ#v=onepage&q=spring%20batch%20.on(%22failed%22)&f=false
				.on("COMPLETED").to(splitSixFlow())
				.on("FAILED").fail() // 이것은, .on("*").to(failureStep())  과 같은 의미이다.
				.from(splitSixFlow()).on("COMPLETED").to(splitTwoFlow())
				.end()
				.build();
	}
	
	//스텝 작업중
//	@Bean
//	public Step monsterPreviewHrefCrawlStep() {
//		return stepBuilderFactory.get("monsterPreviewHrefCrawlStep")
//		.tasklet((contribution, chunkContext) -> {
//			System.out.println("monsterPreviewHrefCrawlStep...");
//			MonsterCrawler.monsterPreviewHrefCrawl();
//			return RepeatStatus.FINISHED;
//		})
//		
//		.build();
//	}
	
	@Bean
	public Step monsterPreviewHrefCrawlStep() {
		//Step 객체 생성
		return stepBuilderFactory.get("monsterPreviewHrefCrawlStep")
				.<String, MonsterPreviewHrefCrawlStepDto>chunk(6)
				.reader(monsterPreviewHrefCrawlReader()) // 데이터 읽기
				.processor(monsterPreviewHrefCrawlProcess()) // 읽은 데이터 처리
				.writer(monsterPreviewHrefCrawlWriter()) // 처리한 데이터 DB에 저장
				.faultTolerant()
				.retryLimit(3)
				.retry(IOException.class)
				.retry(FileNotFoundException.class)
				.build();
	}
	
	@Bean
	@StepScope
	public ItemReader<String> monsterPreviewHrefCrawlReader() {
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		
		List<String> list =  Arrays.asList(attriStrs);

		return new ListItemReader<String>(list);
	}
	
	@Bean
	@StepScope
	public ItemProcessor<String, MonsterPreviewHrefCrawlStepDto> monsterPreviewHrefCrawlProcess() {
		return new ItemProcessor<String, MonsterPreviewHrefCrawlStepDto>() {

			@Override
			public MonsterPreviewHrefCrawlStepDto process(String attriStr) throws Exception {
				
				//해당 속성 기준 7,6,5 성에대한 href 와 attriStr이 담긴 dto을 리턴.
				return MonsterPreviewHrefCrawlDivide.getMonsterPreviewHrefCrawlStepDto(attriStr);
			}

		};
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterPreviewHrefCrawlStepDto> monsterPreviewHrefCrawlWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterPreviewHrefCrawlStepDto> items) throws Exception {
				for (MonsterPreviewHrefCrawlStepDto monsterPreviewHrefCrawlStepDto : items) {					
					//dto 대로 txt 파일 만들기
					MonsterPreviewHrefCrawlDivide.printTxtFileFromhrefRsltMap(monsterPreviewHrefCrawlStepDto);
				}
			}

		};
	}
	
	
	
	
	
	
	@Bean
	public Step monsterScreenshotUrlCrawlStep() {
//		return stepBuilderFactory.get("monsterScreenshotUrlCrawlStep").tasklet((contribution, chunkContext) -> {
//			System.out.println("monsterScreenshotUrlCrawlStep...");
//			MonsterCrawler.monsterScreenshotUrlCrawlAndDownload();
//			return RepeatStatus.FINISHED;
//		}).build();
		
		return stepBuilderFactory.get("monsterScreenshotUrlCrawlStep")
				.tasklet(monsterScreenshotUrlCrawlTasklet())
				.build();
	}
	
	@Bean
	public MonsterScreenshotUrlCrawlTasklet monsterScreenshotUrlCrawlTasklet() {
		MonsterScreenshotUrlCrawlTasklet monsterScreenshotUrlCrawlTasklet = new MonsterScreenshotUrlCrawlTasklet();
		return monsterScreenshotUrlCrawlTasklet;
	}
	
	
	@Bean
	public Step monsterPreviewIconUrlCrawlStep() {
		return stepBuilderFactory.get("monsterPreviewIconUrlCrawlStep").tasklet((contribution, chunkContext) -> {
			System.out.println("monsterPreviewIconUrlCrawlStep...");
			MonsterCrawler.monsterPreviewIconUrlCrawl();
			return RepeatStatus.FINISHED;
		}).build();
	}
	
	@Bean
	public Step monsterEvolutionIconUrlCrawlStep() {
		return stepBuilderFactory.get("monsterEvolutionIconUrlCrawlStep").tasklet((contribution, chunkContext) -> {
			System.out.println("monsterEvolutionIconUrlCrawlStep...");
			MonsterCrawler.monsterEvolutionIconUrlCrawl();
			return RepeatStatus.FINISHED;
		}).build();
	}
	
	@Bean
	public Step getObjectUrlAndSetMonsterDtoAndInsertDBstep() {
		//Step 객체 생성
		return stepBuilderFactory.get("getObjectUrlAndSetMonsterDtoAndInsertDBstep")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, MonsterDto>chunk(10)
				.reader(MonsterDtoitemReader()) // 데이터 읽기
				.processor(MonsterDtoitemProcess()) // 읽은 데이터 처리
				.writer(MonsterDtoitemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemReader<String> MonsterDtoitemReader() {
		List<String> list = new ArrayList<>();
		
		list = MonsterDtoCrawlerDivide.getMonsterDetailPageUrlList();

		return new ListItemReader<String>(list);
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemProcessor<String, MonsterDto> MonsterDtoitemProcess() {
		return new ItemProcessor<String, MonsterDto>() {

			@Override
			public MonsterDto process(String monsterDetailPageUrl) throws Exception {
				
				//파싱해서 만든 dto 를 리턴.
				return MonsterDtoCrawlerDivide.getMonsterDtoFromDetailPageCrawl(monsterDetailPageUrl);
			}

		};
	}
	
	@Autowired private MonsterDao monsterDao;
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterDto> MonsterDtoitemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterDto> items) throws Exception {
				for (MonsterDto monsterDto : items) {					
					monsterDao.insertMonsterInfo(monsterDto);
				}
			}

		};
	}
	
	@Bean
	public Step getObjectUrlAndSetMonsterEvolutionDtoAndInsertDBstep() {
		//Step 객체 생성
		return stepBuilderFactory.get("getObjectUrlAndSetMonsterEvolutionDtoAndInsertDBstep")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, MonsterEvolutionDto>chunk(10)
				.reader(MonsterEvolutionDtoitemReader()) // 데이터 읽기
				.processor(MonsterEvolutionDtoitemProcess()) // 읽은 데이터 처리
				.writer(MonsterEvolutionDtoitemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemReader<String> MonsterEvolutionDtoitemReader() {
		List<String> list = new ArrayList<>();
		
		list = MonsterDtoCrawlerDivide.getMonsterDetailPageUrlList();

		return new ListItemReader<String>(list);
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemProcessor<String, MonsterEvolutionDto> MonsterEvolutionDtoitemProcess() {
		return new ItemProcessor<String, MonsterEvolutionDto>() {

			@Override
			public MonsterEvolutionDto process(String monsterDetailPageUrl) throws Exception {
				
				//파싱해서 만든 dto 를 리턴.
				return MonsterEvolutionDtoCrawlerDivide.getMonsterEvolutionDtoFromDetailPageCrawl(monsterDetailPageUrl);
			}

		};
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterEvolutionDto> MonsterEvolutionDtoitemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterEvolutionDto> items) throws Exception {
				for (MonsterEvolutionDto monsterEvolutionDto : items) {					
					monsterDao.insertMonsterEvolutionInfo(monsterEvolutionDto);
				}
			}

		};
	}
	
	
	@Bean
	public Step getObjectUrlAndsetMonsterNormalSkillDtoAndInsertDBstep() {
		//Step 객체 생성
		return stepBuilderFactory.get("getObjectUrlAndsetMonsterNormalSkillDtoAndInsertDBstep")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, MonsterNormalSkillDto>chunk(10)
				.reader(MonsterNormalSkillDtoitemReader()) // 데이터 읽기
				.processor(MonsterNormalSkillDtoitemProcess()) // 읽은 데이터 처리
				.writer(MonsterNormalSkillDtoitemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemReader<String> MonsterNormalSkillDtoitemReader() {
		List<String> list = new ArrayList<>();
		
		list = MonsterDtoCrawlerDivide.getMonsterDetailPageUrlList();

		return new ListItemReader<String>(list);
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemProcessor<String, MonsterNormalSkillDto> MonsterNormalSkillDtoitemProcess() {
		return new ItemProcessor<String, MonsterNormalSkillDto>() {

			@Override
			public MonsterNormalSkillDto process(String monsterDetailPageUrl) throws Exception {
				
				//파싱해서 만든 dto 를 리턴.
				return MonsterNormalSkillDtoCrawlerDivide.getMonsterNormalSkillDtoFromDetailPageCrawl(monsterDetailPageUrl);
			}

		};
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterNormalSkillDto> MonsterNormalSkillDtoitemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterNormalSkillDto> items) throws Exception {
				for (MonsterNormalSkillDto monsterNormalSkillDto : items) {					
					monsterDao.insertMonsterNormalSkillInfo(monsterNormalSkillDto);
				}
			}

		};
	}
	
	@Bean
	public Step getObjectUrlAndsetMonsterChainSkillDtoAndInsertDBstep() {
		//Step 객체 생성
		return stepBuilderFactory.get("getObjectUrlAndsetMonsterChainSkillDtoAndInsertDBstep")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, MonsterChainSkillDto>chunk(10)
				.reader(MonsterChainSkillDtoitemReader()) // 데이터 읽기
				.processor(MonsterChainSkillDtoitemProcess()) // 읽은 데이터 처리
				.writer(MonsterChainSkillDtoitemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemReader<String> MonsterChainSkillDtoitemReader() {
		List<String> list = new ArrayList<>();
		
		list = MonsterDtoCrawlerDivide.getMonsterDetailPageUrlList();

		return new ListItemReader<String>(list);
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemProcessor<String, MonsterChainSkillDto> MonsterChainSkillDtoitemProcess() {
		return new ItemProcessor<String, MonsterChainSkillDto>() {

			@Override
			public MonsterChainSkillDto process(String monsterDetailPageUrl) throws Exception {
				
				//파싱해서 만든 dto 를 리턴.
				return MonsterChainSkillDtoCrawlerDivide.getMonsterChainSkillDtoFromDetailPageCrawl(monsterDetailPageUrl);
			}

		};
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterChainSkillDto> MonsterChainSkillDtoitemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterChainSkillDto> items) throws Exception {
				for (MonsterChainSkillDto monsterChainSkillDto : items) {					
					monsterDao.insertMonsterChainSkillInfo(monsterChainSkillDto);
				}
			}

		};
	}
	
	@Bean
	public Step getObjectUrlAndsetMonsterLeaderSkillDtoAndInsertDBstep() {
		//Step 객체 생성
		return stepBuilderFactory.get("getObjectUrlAndsetMonsterLeaderSkillDtoAndInsertDBstep")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, MonsterLeaderSkillDto>chunk(10)
				.reader(MonsterLeaderSkillDtoitemReader()) // 데이터 읽기
				.processor(MonsterLeaderSkillDtoitemProcess()) // 읽은 데이터 처리
				.writer(MonsterLeaderSkillDtoitemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemReader<String> MonsterLeaderSkillDtoitemReader() {
		List<String> list = new ArrayList<>();
		
		list = MonsterDtoCrawlerDivide.getMonsterDetailPageUrlList();

		return new ListItemReader<String>(list);
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemProcessor<String, MonsterLeaderSkillDto> MonsterLeaderSkillDtoitemProcess() {
		return new ItemProcessor<String, MonsterLeaderSkillDto>() {

			@Override
			public MonsterLeaderSkillDto process(String monsterDetailPageUrl) throws Exception {
				
				//파싱해서 만든 dto 를 리턴.
				return MonsterLeaderSkillDtoCrawlerDivide.getMonsterLeaderSkillDtoFromDetailPageCrawl(monsterDetailPageUrl);
			}

		};
	}
	
	@Bean
	//@JobScope
	@StepScope
	public ItemWriter<MonsterLeaderSkillDto> MonsterLeaderSkillDtoitemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends MonsterLeaderSkillDto> items) throws Exception {
				for (MonsterLeaderSkillDto monsterLeaderSkillDto : items) {					
					monsterDao.insertMonsterLeaderSkillInfo(monsterLeaderSkillDto);
				}
			}

		};
	}

	///////////////////////////////////////////////////////////////////////////

	//Step 안에 Tasklet 혹은 Reader & Processor & Writer 묶음이 존재.
	//Tasklet 하나와 Reader & Processor & Writer 한 묶음이 같은 레벨
	//(Reader & Processor 가 끝나고 Tasklet으로 마무리 지을 수 없음!!)
	
	//version1. tasklet 미사용
	//Step : 실질적인 배치 처리를 정의하고 제어 하는데 필요한 모든 정보가 있는 도메인 객체
	@Bean
	public Step step() {
		//Step 객체 생성
		return stepBuilderFactory.get("simple-step")
				//chunk :  각 커밋 사이에 처리되는 row 수
				//한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
				//Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우엔 해당 Chunk 만큼만 롤백
				//10개씩 묶어서 처리한다.
				.<String, StringWrapper>chunk(10).reader(itemReader()) // 데이터 읽기
				.processor(itemProcess()) // 읽은 데이터 처리
				.writer(itemWriter()) // 처리한 데이터 DB에 저장
				.build();
	}

	//version2. tasklet 사용
	//Tasklet : 각 Step에서 수행되는 로직 (Spring Batch에서 미리 정의해놓은 Reader, Processor, Writer Interface 사용가능)
	//Tasklet은 Step 안에서 단일로 수행될 커스텀한 기능들을 선언할 때 사용한다.
	// chunk (reader + processor + writer) 를 사용하면 읽기 -> 가공하기 -> 쓰기의 반복인데 chunk 없이 한 번만 실행하도록 하는 것도 가능하다.
	@Bean
	public Step simpleStep1() {
		return stepBuilderFactory.get("simpleStep1").tasklet((contribution, chunkContext) -> {
			System.out.println("에베베베ㅔ");
			return RepeatStatus.FINISHED;
		}).build();
	}

	//@JobScope : Job이 실행될때 생성시킨다.
	//@StepScope : Step이 실행될때 생성시킨다.
	//잡이 실행될때는 내부적으로 jobExecutionContext 를 생성 하는데, 버전(날짜시간개념)이 jobExecutionContext 정보에 들어있다. Scope를 주지않으면, 갱신이 안되서 이미 실행된 job으로 판단하기때문에, 잡실행을 아예 하지않으므로, Scope 를 둘중에 아무거나 줘야한다. 
	//Step의 ItemReader는 ArrayList에 100개의 String value를 담고 있다. (읽기)
	@Bean
	//@StepScope
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
	//private ItemProcessor<String, StringWrapper> itemProcess() {
	//return StringWrapper::new;
	//}

	@Bean
	//@StepScope
	@JobScope
	public ItemProcessor<String, StringWrapper> itemProcess() {
		return new ItemProcessor<String, StringWrapper>() {

			@Override
			public StringWrapper process(String item) throws Exception {
				return new StringWrapper(item);
			}

		};
	}

	public class SimpleProcessor implements ItemProcessor<String, StringWrapper> {

		@Override
		public StringWrapper process(String item) throws Exception {
			return new StringWrapper(item);
		}

	}

	//ItemWriter는 ItemProcessor를 통해 StringWrapper로 반환된 List를 System.out.println으로 로그를 찍는다. (쓰기)
	//private ItemWriter<StringWrapper> itemWriter() {
	//return System.out::println;
	//}
	@Bean
	//@StepScope
	@JobScope
	public ItemWriter<StringWrapper> itemWriter() {
		return new ItemWriter<>() {

			@Override
			public void write(List<? extends StringWrapper> items) throws Exception {
				//for(StringWrapper wrapper : items) {
				//System.out.println(wrapper.value);
				//}
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
