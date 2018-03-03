
function ready(fn) {
  if (document.readyState != 'loading'){
    fn();
  } else if (document.addEventListener) {
    document.addEventListener('DOMContentLoaded', fn);
  } else {
    document.attachEvent('onreadystatechange', function() {
      if (document.readyState != 'loading')
        fn();
    });
  }
}

function addEventListener1(el, eventName, handler) {
   
  if (el.addEventListener) {
    el.addEventListener(eventName, handler);
  } else {
    el.attachEvent('on' + eventName, function(){
      handler.call(el);
    });
  }
}

ready(function(){
    var wrap = document.getElementById("_timeout")
    var href = document.getElementById("relogin");
    var step1 = document.getElementById("step1");
    var step2 = document.getElementById("step2");
    var submit = document.getElementById("submit");
    var href_handdle = function(){
       step1.style.display = "none";
       step2.style.display = "block";
    }
    var submit_handle = function(){
        var account = document.getElementById("account").value,
            pwd     = document.getElementById("pwd").value,
            tip     = document.getElementById("timeoutip");
        pwd = hex_md5(pwd);
        
        var part = [];
        part.push(encodeURIComponent("username") + "=" + encodeURIComponent(account));
        part.push(encodeURIComponent("password") + "=" + encodeURIComponent(pwd));
        var data = part.join("&");

        var request = new XMLHttpRequest();
        request.open('POST', '/api/login', true);
        request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        request.send(data);

        request.onreadystatechange = function(){
            if (this.readyState === 4) {
                if (this.status >= 200 && this.status < 400) {
                    // Success!
                    console.log(this)
                    var resp = JSON.parse(this.responseText);
                    if(resp.code != 0){
                        
                        tip.style.visibility = "visible";
                    } else {
                        tip.style.visibility = "hidden";
                        wrap.style.display = "none";
                    }
                    
                } else {
                    // Error :(
                }
            }
        }
    }

    addEventListener1(href, "click", href_handdle);
    addEventListener1(submit, "click", submit_handle);
})