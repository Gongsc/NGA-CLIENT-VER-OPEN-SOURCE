package gov.anzong.androidnga.activity;

import android.os.Bundle;


import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.ui.compose.SearchScreenKt;

/** Java route host retained so ARouter's Java annotation processor sees the route. */
public class SearchActivity extends BaseActivity {
    public static final String SEARCH_MODE_USER = "1";
    public static final String SEARCH_MODE_TOPIC = "2";
    public static final String SEARCH_MODE_BOARD = "3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        SearchScreenKt.installSearchContent(this);
    }

    @Override
    protected boolean usesComposeContent() {
                return true;
            }
        }
