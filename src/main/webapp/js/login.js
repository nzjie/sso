(function(){
	var verifyKey;
	$(document).ready(function(){
		getVerifykey();
	})
	
	var selectBar = $("#iSelectBar");
	var slider = SliderFactory.createSlider(iForms,iForms,{
		autoCtrl: true,
		pageCount: 2,
		animateDuration:"0.2",//动画过渡时间
		animateType: "ease",//过渡效果
	}).success(function(ret,wrap,pageInfo){
		handleSelectBar(pageInfo);
	}).move(function(ret,wrap,pageInfo){
		var left = (-(ret.distX)) / 2;//因为选择导航占屏幕的一半，所以要除以2
		selectBar.css({
			left:left
		})
	}).recovery(function(ret,wrap,pageInfo){
		handleSelectBar(pageInfo);
	});
	
	function handleSelectBar(pageInfo){
		var idx = Math.abs(pageInfo.getIdx()) || 0;
		var left = 50 * idx;
		selectBar.css({
			left: left+"%"
		})
	}
	
	$(".submitBtn").on("click" , function(){
		var url = "dologin.do";
		var _this  = $(this);
		var parent = _this.parent();
		var name = $.trim(parent.find("input[name=key]").val());
		var password = $.trim(parent.find("input[name=password]").val());
		var type = _this.attr("data-type");
		var confirmPassword ;
		var verifycode;
		if(type == "register"){
			url = "register.do";
			confirmPassword = $.trim(parent.find("input[name=confirmPasswd]").val());
			verifycode = $.trim(parent.find("input[name=vertifyCode]").val());
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
			if(!verifycode){
				$.showToast("验证码为空");
				return;
			}
			
		}
		var loading = $.showloading(type == "register" ? "注册中":"正在登录");
		$.ajax({
			type: "post",
			url: url,
			data:{
				key:name,
				password: password,
				verifycode: verifycode,
				verifyKey:verifyKey
			},
			success: function(data){
				if(data.code != 200){
					$.showToast(data.msg,2000)
					getVerifykey();//更新验证码
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
		var type = _this.attr("data-type");
		if(type == "login"){
			selectBar.css("left",0);
			$(".form-group").css("transform","translateX(0%)")
			slider.setPageIdx(0,-1);
		}else{
			$(".form-group").css("transform","translateX(-50%)")
			selectBar.css("left","50%");
			slider.setPageIdx(-1,-1);
		}
	});
	
	$("#iRegisterForm").on("click",".verifyImg",function(){
		getVerifykey();
	})
	
	function getVerifykey(){
		var ele = $("#iRegisterForm").find(".verifyImg");
		var p = ele.parent();
		ele.remove();
		$.ajax({
			type: 'get',
			url: 'getverifykey.do',
			data: {
				key:verifyKey
			},
			success: function(data){
				verifyKey = data.key;
				p.html("<img class='verifyImg' src='getvertifycode.do?key="+verifyKey+"'>");
			}
		})
	}
	
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