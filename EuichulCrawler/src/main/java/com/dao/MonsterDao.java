package com.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dto.MonsterChainSkillDto;
import com.dto.MonsterDto;
import com.dto.MonsterEvolutionDto;
import com.dto.MonsterLeaderSkillDto;
import com.dto.MonsterNormalSkillDto;

@Component
public class MonsterDao {
	@Autowired
	private SqlSession sqlSession;
	
	public void insertMonsterInfo(MonsterDto dto) {
		this.sqlSession.insert("MonsterInfo.insertMonsterInfo", dto);
	}
	
	public void insertMonsterEvolutionInfo(MonsterEvolutionDto dto) {
		this.sqlSession.insert("MonsterEvolutionInfo.insertMonsterEvolutionInfo", dto);
	}
	
	public void insertMonsterNormalSkillInfo(MonsterNormalSkillDto dto) {
		this.sqlSession.insert("MonsterNormalSkillInfo.insertMonsterNormalSkillInfo", dto);
	}
	
	public void insertMonsterChainSkillInfo(MonsterChainSkillDto dto) {
		this.sqlSession.insert("MonsterChainSkillInfo.insertMonsterChainSkillInfo", dto);
	}
	
	public void insertMonsterLeaderSkillInfo(MonsterLeaderSkillDto dto) {
		this.sqlSession.insert("MonsterLeaderSkillInfo.insertMonsterLeaderSkillInfo", dto);
	}
	
	public int getMonsterId(String name) {
		int rslt = (Integer)this.sqlSession.selectOne("MonsterInfo.getMonsterId", name);
		return rslt;
	}
}
