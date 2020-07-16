package com.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dto.TowerBossDto;
import com.dto.TowerBossRewardDto;
import com.dto.TowerDto;
import com.dto.TowerFloorDto;
import com.dto.TowerFloorMonsterDto;

@Component
public class TowerDao {
	@Autowired
	private SqlSession sqlSession;
	
	public void insertTowerInfo(TowerDto dto) {
		this.sqlSession.insert("TowerInfo.insertTowerInfo", dto);
	}
	
	public void insertTowerBossInfo(TowerBossDto dto) {
		this.sqlSession.insert("TowerBossInfo.insertTowerBossInfo", dto);
	}
	
	public void insertTowerBossRewardInfo(TowerBossRewardDto dto) {
		this.sqlSession.insert("TowerBossRewardInfo.insertTowerBossRewardInfo", dto);
	}
	
	public void insertTowerFloorInfo(TowerFloorDto dto) {
		this.sqlSession.insert("TowerFloorInfo.insertTowerFloorInfo", dto);
	}
	
	public void insertTowerFloorMonsterInfo(TowerFloorMonsterDto dto) {
		this.sqlSession.insert("TowerFloorMonsterInfo.insertTowerFloorMonsterInfo", dto);
	}
	
	public int getTowerId(String name) {
		int rslt = (Integer)this.sqlSession.selectOne("TowerInfo.getTowerId", name);
		return rslt;
	}
	
	public int getLayer(int layer) {
		int rslt = (Integer)this.sqlSession.selectOne("TowerBossInfo.getLayer", layer);
		return rslt;
	}
	
	public int getFloorNum(int floorNum) {
		int rslt = (Integer)this.sqlSession.selectOne("TowerFloorInfo.getFloorNum", floorNum);
		return rslt;
	}
	
	public void updateTowerBossInfoName(int layer, String name) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("layer", layer);
		paramMap.put("name", name);
		
		this.sqlSession.update("TowerBossInfo.updateTowerBossInfoName", paramMap);
		
	}
}
