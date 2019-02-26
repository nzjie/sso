(function(){
	$(".submitBtn").on("click" , function(){
		var url = "dologin.do";
		var _this  = $(this);
		var parent = _this.parent();
		var name = $.trim(parent.find("input[name=key]").val());
		var password = $.trim(parent.find("input[name=password]").val());
		var type = _this.attr("data-type");
		var confirmPassword ;
		if(type == "register"){
			url = "register.do";
			confirmPassword = $.trim(parent.find("input[name=confirmPasswd]").val());
		}
		if(!name){
			$.showToast("用户名不能为空")
			return;
		}
		if(!password){
			$.showToast("密码不能为空");
			return;
		}
		if(type == "register"){
			if( !confirmPassword){
				$.showToast("请输入确认密码");
				return;
			}
			if(confirmPassword != password){
				$.showToast("两次密码不一致");
				return;
			}
			
		}
		var loading = $.showloading("正在登录");
		$.ajax({
			type: "post",
			url: url,
			data:{
				key:name,
				password: password
			},
			success: function(data){
				if(data.code != 200){
					$.showToast(data.msg,2000)
					return;
				}
				loading.hide();
				if(ref){
					location.href = ref;
				}else{
					var host = location.host;
					if(host.indexOf("ajie18")>-1){
						location.href = "http://www.ajie18.top/blog/index.do";
					}else{
						location.href = "http://localhost:8080/blog/index.do";
					}
				}
				
			},
			fail: function(e){
				$.showToast(e);
			},
			complete: function(){
				
			}
			
		})
	})
	
	$(".navBar").on("click","div" , function(){
		var _this = $(this);
		if(_this.hasClass("active")){
			return;
		}
		_this.siblings("div").removeClass("active");
		_this.addClass("active");
		var type = _this.attr("data-type");
		if(type == "login"){
			$(".form-group").css("transform","translateX(0%)")
		}else{
			$(".form-group").css("transform","translateX(-50%)")
		}
	})
	
})()

/**
 * 检验用户名是否已使用
 */
function verify(input){
	var parent  = $(input).parent();
	parent.siblings(".exsit-name").addClass("hidden");//先去除提示
	var name = $(input).val();
	if(!name || !name .length){
		return;
	}
	$.ajax({
		type: "post",
		url: "verifyusername.do",
		data:{
			name:name,
		},
		success: function(data){
			console.log(data);
			if(data.code != 200){
				parent.siblings(".exsit-name").removeClass("hidden");
			}
		},
		fail: function(e){
			console.log(e);
		},
		complete: function(){
			
		}
		
	})
}