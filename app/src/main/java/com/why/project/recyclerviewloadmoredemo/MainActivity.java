package com.why.project.recyclerviewloadmoredemo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.why.project.recyclerviewloadmoredemo.adapter.NewsAdapter;
import com.why.project.recyclerviewloadmoredemo.bean.NewsBean;
import com.why.project.recyclerviewloadmoredemo.recyclerview.WRecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private Context mContext;

	/**下拉刷新组件*/
	private SwipeRefreshLayout swipe_container;

	private WRecyclerView mRecyclerView;
	private ArrayList<NewsBean> listitemList;
	private NewsAdapter mNewsAdapter;

	private int curPageIndex = 1;//当前页数
	private int totalPage = 1;//总页数
	private int pageSize = 10;//每一页的列表项数目
	private int position = 1;//序号

	private LinearLayout nodata_layout;//暂无数据区域

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;

		initViews();
		initSwipeRefreshView();//初始化SwipeRefresh刷新控件

		initDatas();

	}

	private void initViews() {
		mRecyclerView = findViewById(R.id.recycler_view);
		nodata_layout = findViewById(R.id.nodata_layout);
	}

	/**
	 * 初始化SwipeRefresh刷新控件
	 */
	private void initSwipeRefreshView() {
		swipe_container = (SwipeRefreshLayout) findViewById(R.id.list_swiperefreshlayout);
		//设置进度条的颜色主题，最多能设置四种
		swipe_container.setColorSchemeResources(R.color.swiperefresh_color_1,
				R.color.swiperefresh_color_2,
				R.color.swiperefresh_color_3,
				R.color.swiperefresh_color_4);
		//调整进度条距离屏幕顶部的距离 scale:true则下拉的时候从小变大
		swipe_container.setProgressViewOffset(true, 0, dip2px(mContext,10));
	}
	/**
	 * dp转px
	 * 16dp - 48px
	 * 17dp - 51px*/
	private int dip2px(Context context, float dpValue) {
		float scale = context.getResources().getDisplayMetrics().density;
		return (int)((dpValue * scale) + 0.5f);
	}

	private void initDatas() {

		//===================网络请求获取列表=====================
		//解决在第二页的时候进行查询的时候返回到该界面时显示的还是第二页数据
		position = 1;
		curPageIndex = 1;
		mNewsAdapter = null;//将列表适配器置空，否则查询界面点击确定按钮的时候无法更新列表数据
		//初始化集合
		listitemList = new ArrayList<NewsBean>();

		//设置布局管理器
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(linearLayoutManager);

		initListData();
	}

	//初始化第一页数据
	private void initListData() {
		//模拟网络请求返回数据
		getBackDataList(curPageIndex,pageSize);
	}

	private void getBackDataList(int curPageIndex, int pageSize) {
		//模拟网络请求获取数据（一般curPageIndex、pageSize都需要传过去，这里是模拟，所以没有用到pageSize）
		String responseStr = getStringFromAssert(MainActivity.this,"pagenum"+curPageIndex + ".txt");

		//模拟网络请求的回调方法
		onBefore();

		onResponse(responseStr);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				onAfter();//模拟网络请求一定的延迟后执行
			}
		},2000);

	}

	private void onBefore() {
		if(!swipe_container.isRefreshing()) {//实现当下拉刷新的时候，不需要显示加载对话框
			Toast.makeText(mContext,"显示加载框",Toast.LENGTH_SHORT).show();
			//showProgressDialog();//显示进度加载框
		}
	}

	private void onResponse(String response) {
		try {
			if (response != null && !"".equals(response) && !"{}".equals(response)){
				if(response.startsWith("jsonp(")){
					response = response.substring(6,response.length() - 1);
				}
				JSONObject responseObj = new JSONObject(response);

				if(responseObj.getString("flag").equals("success")){
					JSONArray listArray = responseObj.getJSONArray("data");
					if(listArray.length() > 0){//如果有筛选功能，则需要使用大于等于，如果没有，只是单纯的刷新，则使用大于即可【不过因为fail的情况下显示无数据区域，所以此处去掉==0的情况】
						//计算总页数
						totalPage = responseObj.getInt("total") % pageSize == 0 ? responseObj.getInt("total") / pageSize : responseObj.getInt("total") / pageSize + 1;//计算总页数
						if(curPageIndex == 1){
							listitemList.clear();//下拉刷新，需要清空集合，因为刷新的是第一页数据【解决下拉刷新后立刻上拉加载崩溃的bug,方案二】
						}
						switchNoDataVisible(false);//显示列表，隐藏暂无数据区域
						for(int i=0;i<listArray.length();i++){
							JSONObject listItemObj = listArray.getJSONObject(i);

							NewsBean newsBean = new NewsBean();
							newsBean.setNewsId(listItemObj.getString("newsId"));
							newsBean.setNewsTitle(listItemObj.getString("newsTitle"));
							listitemList.add(newsBean);
						}
					}else {
						showListFail("数据内容为空");
					}
				}else {
					showListFail("数据内容为空");
				}

			} else {
				showListFail("数据内容为空");
			}
		}catch (JSONException e) {
			Toast.makeText(mContext,"服务器数据解析异常，请联系管理员！",Toast.LENGTH_SHORT).show();
		}catch (Exception e) {
			Toast.makeText(mContext,"服务器数据解析异常，请联系管理员！",Toast.LENGTH_SHORT).show();
		}
	}

	private void onAfter() {
		Toast.makeText(mContext,"隐藏加载框",Toast.LENGTH_SHORT).show();
		//dismissProgressDialog();//隐藏进度加载框

		if(curPageIndex == 1){//如果首页数据为空或者小于每页展现的条数，则禁用上拉加载功能
			if(listitemList.size() < pageSize){
				mRecyclerView.setPullLoadEnable(false);//禁用上拉加载功能
			}else{
				mRecyclerView.setPullLoadEnable(true);//启用上拉加载功能
			}
		}

		//设置适配器
		if(mNewsAdapter == null){
			//设置适配器
			mNewsAdapter = new NewsAdapter(this, listitemList);
			mRecyclerView.setAdapter(mNewsAdapter);
			//添加分割线
			//设置添加删除动画
			//调用ListView的setSelected(!ListView.isSelected())方法，这样就能及时刷新布局
			mRecyclerView.setSelected(true);
		}else{
			mNewsAdapter.notifyDataSetChanged();
		}

		stopRefreshAndLoading();//停止刷新和上拉加载

		//初始化监听事件
		initEvents();
	}

	//获取列表数据失败
	public void showListFail(String msg) {
		//当第一页数据为空的时候，才会执行
		if(curPageIndex == 1){
			//扩展：显示无数据占位图
			switchNoDataVisible(true);
		}else{
			Toast.makeText(mContext,msg,Toast.LENGTH_SHORT).show();
		}
	}

	/**切换无数据和列表展现/隐藏*/
	private void switchNoDataVisible(boolean showNoData){
		if(showNoData){
			nodata_layout.setVisibility(View.VISIBLE);
			swipe_container.setVisibility(View.GONE);
		}else if(nodata_layout.getVisibility() == View.VISIBLE){
			nodata_layout.setVisibility(View.GONE);
			swipe_container.setVisibility(View.VISIBLE);
		}
	}

	private void initEvents() {
		//为SwipeRefreshLayout布局添加一个Listener，下拉刷新
		swipe_container.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshList();//刷新列表
			}
		});

		//自定义上拉加载的监听
		mRecyclerView.setWRecyclerListener(new WRecyclerView.WRecyclerViewListener() {
			@Override
			public void onLoadMore() {
				Log.w(TAG, "onLoadMore-正在加载");
				curPageIndex = curPageIndex + 1;
				if (curPageIndex <= totalPage) {
					initListData();//更新列表项集合
				} else {
					//到达最后一页了
					Toast.makeText(mContext,"我也是有底线滴",Toast.LENGTH_SHORT).show();
					//隐藏正在加载的区域
					stopRefreshAndLoading();
				}
			}
		});

		//列表适配器的点击监听事件
		mNewsAdapter.setOnItemClickLitener(new NewsAdapter.OnItemClickLitener() {
			@Override
			public void onItemClick(View view, int position) {
			}

			@Override
			public void onItemLongClick(View view, int position) {
			}
		});
	}

	/**刷新列表*/
	private void refreshList() {
		mRecyclerView.setPullLoadEnable(false);//禁用上拉加载功能
		mRecyclerView.setPullRefresh(true);//设置处于下拉刷新状态中
		curPageIndex = 1;
		position = 1;
		//listitemList.clear();//下拉刷新，需要清空集合，因为刷新的是第一页数据【解决下拉刷新后立刻上拉加载崩溃的bug,方案二】
		initListData();//更新列表项集合
	}

	/**
	 * 停止刷新和上拉加载
	 */
	private void stopRefreshAndLoading() {
		//检查是否处于刷新状态
		if(swipe_container.isRefreshing()){
			//显示或隐藏刷新进度条，一般是在请求数据的时候设置为true，在数据被加载到View中后，设置为false。
			swipe_container.setRefreshing(false);
		}
		//如果正在加载，则获取数据后停止加载动画
		if(mRecyclerView.ismPullLoading()){
			mRecyclerView.stopLoadMore();//停止加载动画
		}
		mRecyclerView.setPullRefresh(false);//设置处于下拉刷新状态中[否]
	}




	/*===========读取assets目录下的js字符串文件（js数组和js对象）===========*/
	/**
	 * 访问assets目录下的资源文件，获取文件中的字符串
	 * @param filePath - 文件的相对路径，例如："listdata.txt"或者"/www/listdata.txt"
	 * @return 内容字符串
	 * */
	public String getStringFromAssert(Context mContext, String filePath) {

		String content = ""; // 结果字符串
		try {
			InputStream is = mContext.getResources().getAssets().open(filePath);// 打开文件
			int ch = 0;
			ByteArrayOutputStream out = new ByteArrayOutputStream(); // 实现了一个输出流
			while ((ch = is.read()) != -1) {
				out.write(ch); // 将指定的字节写入此 byte 数组输出流
			}
			byte[] buff = out.toByteArray();// 以 byte 数组的形式返回此输出流的当前内容
			out.close(); // 关闭流
			is.close(); // 关闭流
			content = new String(buff, "UTF-8"); // 设置字符串编码
		} catch (Exception e) {
			Toast.makeText(mContext, "对不起，没有找到指定文件！", Toast.LENGTH_SHORT)
					.show();
		}
		return content;
	}
}
