### Bean
1. 对于Bean而言，我的理解是只要是Java的类的就可以称为一个Bean，更用在Spring上，被Spring管理的对象就可以将其称作为Bean。
2. 它不仅仅可以包括对象的属性以及get,set方法，还可以有具体的业务逻辑。

### POJO
1. pure old java object
2. 简单的Java对象或者无规则简单java对象，但是和JavaBean的不同是没有业务逻辑
3. 一个中间对象，可以转化为PO、DTO、VO。 
    * POJO持久化之后==〉PO
    * POJO传输过程中==〉DTO
    * POJO用作表示层==〉VO
4. PO 和VO都应该属于它。

### PO
1. persistent object
2. 持久对象，可以用来对照数据库中的一条记录

### VO
1. value object值对象 / view object表现层对象
    * 主要对应页面显示（web页面/swt、swing界面）的数据对象。
    * 可以和表对应，也可以不，这根据业务的需要。
    * 注 ：在struts中，用ActionForm做VO，需要做一个转换，因为PO是面向对象的，而ActionForm是和view对应的，要将几个PO要显示的属性合成一个ActionForm，可以使用BeanUtils的copy方法。

### DTO
1. 也叫TO,   Data Transfer Object数据传输对象
  1.用在需要跨进程或远程传输时，它不应该包含业务逻辑。
  2.比如一张表有100个字段，那么对应的PO就有100个属性（大多数情况下，DTO 内的数据来自多个表）。但view层只需显示10个字段，没有必要把整个PO对象传递到client，这时我们就可以用只有这10个属性的DTO来传输数据到client，这样也不会暴露server端表结构。到达客户端以后，如果用这个对象来对应界面显示，那此时它的身份就转为VO。

### Entity
1. 数据表对应到实体类的映射
