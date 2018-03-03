/*
 *  lineChart.js
 *  动态生成折线图
 *  author: csywweb
 *  link : https://github.com/csywweb/lineChart.js 
 *
 */

(function(window, document, undefined) {
  /*
   *  引用自 hidpi-canvas-polyfill
   *  https://github.com/jondavidjohn/hidpi-canvas-polyfill
   *  解决高清屏幕下canvas的模糊问题
   */
  var lastTime = 0;
  var vendors = ["webkit", "moz"];
  for (var x = 0; x < vendors.length && !window.requestAnimationFrame; ++x) {
    window.requestAnimationFrame = window[vendors[x] + "RequestAnimationFrame"];
    window.cancelAnimationFrame =
      window[vendors[x] + "CancelAnimationFrame"] || // name has changed in Webkit
      window[vendors[x] + "CancelRequestAnimationFrame"];
  }

  if (!window.requestAnimationFrame) {
    window.requestAnimationFrame = function(callback, element) {
      var currTime = new Date().getTime();
      var timeToCall = Math.max(0, 16.7 - (currTime - lastTime));
      var id = window.setTimeout(function() {
        callback(currTime + timeToCall);
      }, timeToCall);
      lastTime = currTime + timeToCall;
      return id;
    };
  }
  if (!window.cancelAnimationFrame) {
    window.cancelAnimationFrame = function(id) {
      clearTimeout(id);
    };
  }

  /*
   * 工具类
   * 
   *
   */
  utils = {
    isObject: function(obj) {
      return (
        obj !== null &&
        typeof obj === "object" &&
        Object.getPrototypeOf(obj) === Object.prototype
      );
    },
    autofixString : function(val){
      var str = val + "";
      if(str.length < 2){
        str = "0" + str;
      }
      return str;
    },
    extend: function(target, obj) {
      for (var k in obj) {
        var src = target[k];
        var copy = obj[k];
        if (src === copy) {
          continue;
        }
        if (this.isObject(copy)) {
          src = src || {};
          target[k] = this.extend(src, copy);
        } else {
          target[k] = copy;
        }
      }
      return target;
    },
    getNum: function(string) {
      var value = string.replace(/[^0-9]/gi, "");
      return value;
    },
    getWinWidth: function() {
      var winWidth;
      // 获取窗口宽度
      if (window.innerWidth) winWidth = window.innerWidth;
      else if (document.body && document.body.clientWidth)
        winWidth = document.body.clientWidth;

      return winWidth;
    },
    getWinHeight: function() {
      var winHeight;
      if (window.innerHeight) winHeight = window.innerHeight;
      else if (document.body && document.body.clientHeight)
        winHeight = document.body.clientHeight;

      return winHeight;
    },
    getPixelRatio: function(context) {
      var backingStore =
        context.backingStorePixelRatio ||
        context.webkitBackingStorePixelRatio ||
        context.mozBackingStorePixelRatio ||
        context.msBackingStorePixelRatio ||
        context.oBackingStorePixelRatio ||
        context.backingStorePixelRatio ||
        1;

      return (window.devicePixelRatio || 1) / backingStore;
    },
    getMaximin: function(arr, maximin) {
      if (maximin == "max") {
        return Math.max.apply(Math, arr);
      } else if (maximin == "min") {
        return Math.min.apply(Math, arr);
      }
    }
  };

  var LineChart = {
    init: function(selector) {
      return new CreateChart(selector);
    }
  };
  function CreateChart(selector) {
    this.selector = selector;
    this.target = document.getElementById(selector);
    this.target.style.position = "relative";
    this.appendDom();
    // 默认配置项
    this.config = {
      color: {
        extendsLine: "#DDD",
        Axis: "#333",
        line: "#FF0000",
        fontColor: "#333"
      },
      isX: true,
      isY: true
    };
    //生成canvas对象
    var canvas = document.createElement("canvas");
    canvas.setAttribute("style", "cursor:pointer");
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    //设备缩放比
    var radio = utils.getPixelRatio(this.ctx);
    this.radio = radio;

    var obj = this.target,
      style;
    if (window.getComputedStyle) {
      style = window.getComputedStyle(obj, null);
    } else {
      style = obj.currentStyle;
    }
    //宽度高度， 带单位px
    this.width =
      utils.getNum(style.width) -
      utils.getNum(style.paddingLeft) -
      utils.getNum(style.paddingRight) +
      "px";
    this.height =
      utils.getNum(style.height) -
      utils.getNum(style.paddingTop) -
      utils.getNum(style.paddingBottom) +
      "px";

    this.paddingLeft = utils.getNum(style.paddingLeft);
    this.paddingTop = utils.getNum(style.paddingTop);

    //宽度高度， 不带px
    this.width_num = utils.getNum(this.width);
    this.height_num = utils.getNum(this.height);

    //定义样式
    canvas.width = this.width_num * radio;
    canvas.height = this.height_num * radio;
    canvas.style.width = this.width;
    canvas.style.height = this.height;
  }
  /*
  *   options
  *   
  *
  */
  CreateChart.prototype.filterOption = function(opts) {
    var o = opts;
    this.XTotalData = opts.xAxis.data;
    this.YTotalData = opts.yAxis.data;
    var map_month = {
      "01": "Jan",
      "02": "Feb",
      "03": "Mar",
      "04": "Apr",
      "05": "May",
      "06": "Jun",
      "07": "July",
      "08": "Aug",
      "09": "Sep",
      "10": "Oct",
      "11": "Nov",
      "12": "Dec"
    };
    var date = [];
    
    for(var i = 0; i < o.xAxis.data.length; i++){
      date[i] = map_month[o.xAxis.data[i]];
    }

    //如果传一个值， 自动补全
    if(o.xAxis.data.length == 1){
      var month =o.xAxis.data[0] * 1 -1 ;
      this.XTotalData.unshift(month);
      var m_ = utils.autofixString(month);
     
      date.unshift(map_month[m_]);
    }
    if(o.yAxis.data.length == 1){
      o.yAxis.data.unshift(0);
      //this.YTotalData.unshift(0);
    }
    o.xAxis.data = date;
    return o;
  },
  CreateChart.prototype.setOption = function(options) {
    var opt_ = this.filterOption(options); //过滤日期
    this.options = utils.extend(this.config, opt_);
    this.sets = []; //所有坐标集合

    this.initAxis();
    if (this.xSize > this.yLength) {
      this.xSize = this.yLength;
    } else if (this.xSize < this.yLength) {
      this.yLength = this.xSize;
    }

    var canvas = this.canvas;
    this.target.appendChild(canvas);

    this.setXAxis(); //设定x轴的下标
    this.lineXAxis(); //x轴对应下标的延长线
    if (this.options.isX) {
      this.drawXAxis(); //绘制x轴
    }
    if (this.options.isY) {
      this.drawYAxix(); //绘制y轴
    }
    this.drawLine(); //绘制折线
  };
  CreateChart.prototype.initAxis = function() {
    //坐标原点
    this.startX = 30;
    this.startY = this.height_num * this.radio - 30;
    //x轴的最大坐标点
    this.x_max = this.width_num * this.radio - 30;
    //x轴长度
    this.x_length = this.x_max - this.startX;

    this.xAxis_data = this.options.xAxis.data;
    this.yAxis_data = this.options.yAxis.data;
    this.yLength = this.yAxis_data.length; //y轴数据的个数
    this.xSize = this.xAxis_data.length; //x轴数据的个数
    this.xAxisUnit = parseInt(this.x_length / (this.xSize - 1)); //x轴单位刻度
  };
  CreateChart.prototype.setXAxis = function() {
    var length = this.xSize > 1? this.xSize : 2;
    var unit = this.xAxisUnit;

    this.ctx.fillStyle = this.options.color.fontColor;
    this.ctx.font = 12 * this.radio + "px" + " PingFang SC";
    this.ctx.textAlign = "center";
    for (var i = 0; i < length; i++) {
      var x = this.startX + unit * i;
      var y = this.height_num * this.radio - 18;

      this.ctx.fillText(this.xAxis_data[i], x, y);

      //每个点的X坐标存入数组
      this.sets[i] = [];
      this.sets[i][0] = parseInt(x);
    }
  };
  CreateChart.prototype.lineXAxis = function() {
    var xAxis_data = this.options.xAxis.data;
    var length = this.xAxis_data.length;
    var unit = this.xAxisUnit;

    for (var i = 0; i < length; i++) {
      var x = this.startX + unit * i;
      var y = this.height_num * this.radio - 5;

      this.ctx.beginPath();
      this.ctx.moveTo(x, this.startY);
      this.ctx.lineTo(x, 0);
      this.ctx.lineJoin = "round";
      this.ctx.strokeStyle = this.config.color.extendsLine;
      this.ctx.stroke();
    }
  };
  CreateChart.prototype.drawXAxis = function() {
    var ctx = this.ctx;
    var startX = this.startX;
    var startY = this.startY;
    var endX = this.x_max;
    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineTo(endX, startY);
    ctx.lineJoin = "round";
    ctx.strokeStyle = this.config.color.Axis;
    ctx.stroke();
  };

  CreateChart.prototype.drawYAxix = function() {
    var ctx = this.ctx;
    ctx.beginPath();
    ctx.moveTo(this.startX, this.startY);
    ctx.lineTo(this.startX, 0);
    ctx.lineJoin = "round";
    ctx.strokeStyle = this.config.color.Axis;
    ctx.stroke();
  };

  CreateChart.prototype.drawLine = function() {
    var ydata = this.yAxis_data;
    //找出y轴数据最大点。
   
    var maxData = utils.getMaximin(ydata, "max");
  
    

    //根据Y粥长度算出1个坐标单位代表的数值
    var yUnit = maxData / this.startY;

    //将每个点的y轴坐标存进数组
    for (var i = 0; i < ydata.length; i++) {
      this.sets[i][1] = this.startY - parseInt(ydata[i] / yUnit);
    
    }

    //画线
    this.ctx.beginPath();
    this.ctx.moveTo(this.sets[0][0], this.sets[0][1]);

    for (var j = 1; j < this.sets.length; j++) {
      this.ctx.lineTo(this.sets[j][0], this.sets[j][1]);
    }
    this.ctx.lineJoin = "round";
    this.ctx.lineWidth = 3;
    this.ctx.strokeStyle = this.config.color.line;
    this.ctx.stroke();

    this.initEvent();
  };

  CreateChart.prototype.initEvent = function() {
    var canvas = this.canvas;
    var sets = this.sets; //所有坐标点的集合

    var pop        = document.getElementById(this.selector + "_pop"),
        monthText  = this.monthText,
        numText    = this.numText,
        XTotalData = this.XTotalData,
        YTotalData = this.YTotalData; 

    var paddingLeft = parseInt(this.paddingLeft);
    var paddingTop = parseInt(this.paddingTop);
   
    canvas.onmousemove = function(event) {
      var event = event ? event : window.event;
      var x = event.pageX - canvas.getBoundingClientRect().left;
      var y = event.pageY - canvas.getBoundingClientRect().top;

      for (var i = 0; i < sets.length; i++) {
        if (x < sets[i][0] + 8 && x > sets[i][0] - 8) {
          pop.style.left = parseInt(sets[i][0]) + paddingLeft - 8 + "px";
          pop.style.top = parseInt(sets[i][1]) + paddingTop - 8 + "px";
          pop.style.display = "block";

          monthText.innerText  = XTotalData[i];
          numText.innerText  = YTotalData[i];
          
          break;
        } else {
          pop.style.display = "none";
        }
      }
    };

    canvas.onmouseout = function(event) {
      pop.style.display = "none";
    };
  };

  //绘制DOM，以后会重构一下，临时写的
  CreateChart.prototype.appendDom = function() {
   
    var selector = this.selector;

    var parent = document.createElement("div");
    parent.style.position = "absolute";
    parent.style.display = "none";
    parent.setAttribute("id", selector + "_pop");

    var circle = document.createElement("div");
    circle.setAttribute(
      "style",
      "padding:2px;background-color: #FFF;width:16px;border:1px solid #DDD;border-bottom: 1px solid #FFF;position: relative;top:1px;"
    );

    var circle_ = document.createElement("div");
    circle_.setAttribute(
      "style",
      "width:10px;height:10px;border:1px solid #2ea967;border-radius: 50%;margin:0 auto;"
    );

    circle.appendChild(circle_);
    parent.appendChild(circle);

    var text_grid = document.createElement("div");
    text_grid.setAttribute(
      "style",
      "background-color: rgba(255,255,255, .8);border:1px solid #DDD;padding:4px;width:85px;"
    );

    var p1 = document.createElement("p"),
      p2 = document.createElement("p"),
      span1 = document.createElement("span"),
      span2 = document.createElement("span"),
      text1 = document.createTextNode("月新增客户"),
      text2 = document.createTextNode("人");

    p1.setAttribute("style", "font-size:12px;line-height: 16px;");
    span1.setAttribute("id", selector + "_month");
    p1.appendChild(span1);
    p1.appendChild(text1);

    p2.setAttribute("style", "font-size:14px;line-height: 18px;");
    span2.setAttribute("style", "font-size:16px;");
    span2.setAttribute("id", selector + "_num");
    p2.appendChild(span2);
    p2.appendChild(text2);

    text_grid.appendChild(p1);
    text_grid.appendChild(p2);

    parent.appendChild(text_grid);

    this.target.appendChild(parent);

    this.monthText = document.getElementById(selector + "_month");
    this.numText = document.getElementById(selector + "_num");
  };

  window.LineChart = LineChart;
})(window, document, undefined);
