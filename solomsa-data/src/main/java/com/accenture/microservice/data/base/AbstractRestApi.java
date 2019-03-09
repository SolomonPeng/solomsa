/**
 * 
 */
package com.accenture.microservice.data.base;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.accenture.microservice.app.base.BaseApi;
import com.accenture.microservice.core.GlobalConst;
import com.accenture.microservice.core.util.CoreUtils;
import com.accenture.microservice.data.base.AbstractEntityService;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import springfox.documentation.annotations.ApiIgnore;

/**抽象RestApi类
 * @author song.peng
 *
 * @param <SERVICE> 服务类型
 * @param <T> 必须是以String为主键类型的实体类型
 */
public abstract class AbstractRestApi<SERVICE extends AbstractEntityService<?, T, String>,T extends Persistable<String>> 
	extends BaseApi {

	@Autowired(required=false)
	@Getter
	SERVICE service;
	
	/**根据id获取一个或一组对象
	 * @param id
	 * @return
	 */
	@ApiOperation("根据id查询")
    @GetMapping("/{id}")
	public Object get(@PathVariable String id) {
		Assert.hasText(id, GlobalConst.NULL_ARGUMENT_MSG);
		String[] ids = StringUtils.commaDelimitedListToStringArray(id);
		if (ids.length <= 1) {
			return service.findById(ids[0]);
		}else {
			return service.findByIds(Arrays.asList(ids));
		}
	}
	
	/**根据参数查询记录集
	 * @param map
	 * @return
	 */
	@ApiOperation("根据字段参数查询")
	@GetMapping
	public List<T> get(@RequestParam @ApiIgnore Map<String, Object> paramsMap){
		return service.findByParams(paramsMap);
	}
	
	/**插入保存一个记录
	 * @param map
	 * @return
	 */
	@ApiOperation("新增一个记录")
    @PostMapping
	public T post(@RequestBody Map<String, Object> map) {
		T entity = service.transObject(map);
		Assert.isTrue(entity.isNew(), "不可新增已有对象");
		return service.saveOne(entity);
	}
	
	/**插入保存记录集
	 * @param list
	 * @return
	 */
	@ApiOperation("批量新增记录")
    @PostMapping("/list")
	public List<T> post(@RequestBody List<Map<String, Object>> list){
		Assert.notEmpty(list, "输入集合对象为空");
        List<T> entitys = Lists.newArrayList();
        for(Map<String, Object> item : list) {
        	T entity = service.transObject(item);
        	Assert.isTrue(entity.isNew(), "不可新增已有对象");
        	entitys.add(entity);
        }
    	return service.saveList(entitys);
	}
	
	/**更新保存一个记录
	 * @param map
	 * @return
	 */
	@ApiOperation("更新保存一个记录")
    @PutMapping("/{id}")
	public T put(@PathVariable String id, @RequestBody Map<String, Object> map) {
		Assert.hasText(id, GlobalConst.NULL_ARGUMENT_MSG);
		T entity = service.findById(id);
		Assert.notNull(entity,"不可修改不存在的对象");
		CoreUtils.copyProperties(entity, map);
		return service.saveOne(entity);
	}
	
	/**更新保存记录集
	 * @param list
	 * @return
	 */
	@ApiOperation("批量更新保存记录")
	@PutMapping("/list")
	public List<T> put(@RequestBody List<Map<String, Object>> list){
		Assert.notEmpty(list, "输入集合对象为空");
        List<T> entitys = Lists.newArrayList();
        for(Map<String, Object> item : list) {
        	T entity = service.transObject(item);
        	Assert.isTrue(!entity.isNew(), "不可修改不存在的对象");
        	entitys.add(entity);
        }
    	return service.saveList(entitys);
	}
	
	/**根据id删除一个或一组记录
	 * @param id
	 * @return
	 */
	@ApiOperation("根据id删除记录")
    @DeleteMapping("/{id}")
	public String delete(@PathVariable String id) {
		Assert.hasText(id, GlobalConst.NULL_ARGUMENT_MSG);
		String[] ids = StringUtils.commaDelimitedListToStringArray(id);
    	if (ids.length <= 1) {
    		service.deleteById(ids[0]);
        } else {
        	service.deleteList(Arrays.asList(ids));
        }
        return "删除成功";
	}
}
